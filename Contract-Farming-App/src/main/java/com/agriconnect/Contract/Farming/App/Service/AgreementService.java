package com.agriconnect.Contract.Farming.App.Service;

import com.agriconnect.Contract.Farming.App.DTO.PageRequest;
import com.agriconnect.Contract.Farming.App.DTO.PageResponse;
import com.agriconnect.Contract.Farming.App.DTO.ResponseData;
import com.agriconnect.Contract.Farming.App.Entity.Agreement;
import com.agriconnect.Contract.Farming.App.Repository.AgreementRepository;
import com.agriconnect.Contract.Farming.App.exception.BadRequestException;
import com.agriconnect.Contract.Farming.App.exception.ResourceNotFoundException;
import com.agriconnect.Contract.Farming.App.kafka.NotificationEventPublisher;
import com.agriconnect.Contract.Farming.App.util.CursorUtil;
import com.agriconnect.notification.avro.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class AgreementService {

    private static final Logger logger = LoggerFactory.getLogger(AgreementService.class);

    private final AgreementRepository agreementRepository;
    private final CursorUtil cursorUtil;
    private final NotificationEventPublisher notificationEventPublisher;
    private final CacheService cacheService;

    private static final Duration AGREEMENT_TTL = Duration.ofHours(24);
    private static final Duration ADDRESS_TTL = Duration.ofHours(2);

    @Value("${notification.topics.contract}")
    private String contractTopic;

    @Autowired
    public AgreementService(AgreementRepository agreementRepository,
            CursorUtil cursorUtil,
            NotificationEventPublisher notificationEventPublisher,
            CacheService cacheService) {
        this.agreementRepository = agreementRepository;
        this.cursorUtil = cursorUtil;
        this.notificationEventPublisher = notificationEventPublisher;
        this.cacheService = cacheService;
    }

    public Agreement uploadAgreement(MultipartFile file, String pdfHash, String orderId, String farmerAddress,
            String buyerAddress) throws Exception {
        logger.info("Uploading agreement for order: {}", orderId);

        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        try {
            if (fileName.contains("..")) {
                throw new BadRequestException("Filename contains invalid path sequence: " + fileName);
            }

            Agreement agreement = new Agreement(farmerAddress, buyerAddress, orderId, file.getSize(), fileName,
                    file.getContentType(), file.getBytes(), LocalDate.now(), LocalTime.now(), "", pdfHash);

            Agreement savedAgreement = agreementRepository.save(agreement);

            String downloadURl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/download/")
                    .path(savedAgreement.getPdfHash())
                    .toUriString();

            savedAgreement.setDownloadUrl(downloadURl);

            Agreement finalAgreement = agreementRepository.save(savedAgreement);
            cacheService.evict("agreement:order:" + orderId);
            cacheService.evict("agreement:pdf:" + pdfHash);
            cacheService.evictPattern("agreement:addr:*");
            logger.info("Agreement uploaded successfully with ID: {}", finalAgreement.getId());

            try {
                Map<String, String> p = Map.of(
                        "orderId", orderId,
                        "pdfHash", pdfHash,
                        "uploadedAt", Instant.now().toString());
                notificationEventPublisher.publish(contractTopic,
                        notificationEventPublisher.buildEvent("CONTRACT_AGREEMENT_UPLOADED",
                                farmerAddress, "contract.agreement.uploaded",
                                List.of("EMAIL", "IN_APP"), p, Priority.HIGH, finalAgreement.getId(), null, null));
                notificationEventPublisher.publish(contractTopic,
                        notificationEventPublisher.buildEvent("CONTRACT_AGREEMENT_UPLOADED",
                                buyerAddress, "contract.agreement.uploaded",
                                List.of("EMAIL", "IN_APP"), p, Priority.HIGH, finalAgreement.getId() + "-buyer", null,
                                null));
            } catch (Exception ex) {
                logger.warn("[NOTIFY] CONTRACT_AGREEMENT_UPLOADED failed for agreement={}: {}", finalAgreement.getId(),
                        ex.getMessage());
            }

            return finalAgreement;

        } catch (Exception e) {
            logger.error("Failed to upload agreement: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to upload agreement: " + e.getMessage());
        }
    }

    public Agreement getAgreementByTransactionHash(String transactionHash) {
        logger.debug("Fetching agreement by transaction hash: {}", transactionHash);
        String cacheKey = "agreement:tx:" + transactionHash;
        return cacheService.get(cacheKey, Agreement.class).orElseGet(() -> {
            Agreement agreement = agreementRepository.findByTransactionHash(transactionHash)
                    .orElseThrow(() -> new ResourceNotFoundException("Agreement", "transactionHash", transactionHash));
            cacheService.save(cacheKey, agreement, AGREEMENT_TTL);
            return agreement;
        });
    }

    public Agreement getAgreementByPdfHash(String pdfHash) {
        logger.debug("Fetching agreement by PDF hash: {}", pdfHash);
        String cacheKey = "agreement:pdf:" + pdfHash;
        return cacheService.get(cacheKey, Agreement.class).orElseGet(() -> {
            Agreement agreement = agreementRepository.findByPdfHash(pdfHash)
                    .orElseThrow(() -> new ResourceNotFoundException("Agreement", "pdfHash", pdfHash));
            cacheService.save(cacheKey, agreement, AGREEMENT_TTL);
            return agreement;
        });
    }

    public Agreement getAgreementByOrderId(String orderId) {
        logger.debug("Fetching agreement by order ID: {}", orderId);
        String cacheKey = "agreement:order:" + orderId;
        return cacheService.get(cacheKey, Agreement.class).orElseGet(() -> {
            Agreement agreement = agreementRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Agreement", "orderId", orderId));
            cacheService.save(cacheKey, agreement, AGREEMENT_TTL);
            return agreement;
        });
    }

    public Agreement saveAgreement(Agreement agreement) {
        logger.debug("Saving agreement with ID: {}", agreement.getId());
        Agreement saved = agreementRepository.save(agreement);
        cacheService.evict("agreement:order:" + saved.getOrderId());
        cacheService.evict("agreement:pdf:" + saved.getPdfHash());
        return saved;
    }

    public String deleteAgreement(String pdfHash) {
        logger.info("Deleting agreement with PDF hash: {}", pdfHash);
        Agreement agreement = getAgreementByPdfHash(pdfHash);
        agreementRepository.delete(agreement);
        cacheService.evict("agreement:pdf:" + pdfHash);
        cacheService.evict("agreement:order:" + agreement.getOrderId());
        cacheService.evictPattern("agreement:addr:*");
        return "Agreement with pdf hash " + pdfHash + " has been deleted successfully";
    }

    public String deleteAllAgreements() {
        logger.warn("Deleting all agreements - this is a dangerous operation!");
        agreementRepository.deleteAll();
        cacheService.evictPattern("agreement:*");
        return "All agreements have been deleted successfully";
    }

    public List<Agreement> getAgreementsByAddress(String address) {
        logger.debug("Fetching agreements for address: {}", address);
        String cacheKey = "agreement:addr:" + address;
        return cacheService.get(cacheKey, List.class).orElseGet(() -> {
            List<Agreement> agreements = agreementRepository.findAgreementsByAddress(address);
            cacheService.save(cacheKey, agreements, ADDRESS_TTL);
            return agreements;
        });
    }

    public List<Agreement> getAllAgreements() {
        logger.debug("Fetching all agreements (deprecated - use pagination)");
        return agreementRepository.findAll();
    }

    // ==================== CURSOR-BASED PAGINATION METHODS ====================

    /**
     * Get paginated agreements with cursor-based pagination
     */
    public PageResponse<Agreement> getAgreementsPaginated(PageRequest pageRequest) {
        logger.debug("Fetching paginated agreements - cursor: {}, limit: {}",
                pageRequest.getCursor(), pageRequest.getLimit());

        List<Agreement> agreements;
        int limit = pageRequest.getLimit();
        org.springframework.data.domain.PageRequest springPageRequest = org.springframework.data.domain.PageRequest
                .of(0, limit + 1);

        if (pageRequest.getCursor() == null) {
            // First page
            agreements = agreementRepository.findAgreementsFirstPageDesc(springPageRequest);
        } else {
            // Subsequent pages
            CursorUtil.CursorData cursorData = cursorUtil.decodeCursor(pageRequest.getCursor());
            agreements = agreementRepository.findAgreementsAfterCursorDesc(
                    cursorData.getDate(), cursorData.getTime(), cursorData.getId(), springPageRequest);
        }

        return buildPageResponse(agreements, limit, pageRequest);
    }

    /**
     * Get paginated agreements for a specific address (farmer or buyer)
     */
    public PageResponse<Agreement> getAgreementsByAddressPaginated(String address, PageRequest pageRequest) {
        logger.debug("Fetching paginated agreements for address: {} - cursor: {}, limit: {}",
                address, pageRequest.getCursor(), pageRequest.getLimit());

        List<Agreement> agreements;
        int limit = pageRequest.getLimit();
        org.springframework.data.domain.PageRequest springPageRequest = org.springframework.data.domain.PageRequest
                .of(0, limit + 1);

        if (pageRequest.getCursor() == null) {
            // First page
            agreements = agreementRepository.findAgreementsByAddressFirstPage(address, springPageRequest);
        } else {
            // Subsequent pages
            CursorUtil.CursorData cursorData = cursorUtil.decodeCursor(pageRequest.getCursor());
            agreements = agreementRepository.findAgreementsByAddressAfterCursor(
                    address, cursorData.getDate(), cursorData.getTime(), cursorData.getId(), springPageRequest);
        }

        return buildPageResponse(agreements, limit, pageRequest);
    }

    /**
     * Build page response with metadata
     */
    private PageResponse<Agreement> buildPageResponse(List<Agreement> agreements, int limit, PageRequest pageRequest) {
        boolean hasNext = agreements.size() > limit;

        if (hasNext) {
            agreements = agreements.subList(0, limit);
        }

        String nextCursor = null;
        String prevCursor = null;

        if (!agreements.isEmpty()) {
            if (hasNext) {
                Agreement lastAgreement = agreements.get(agreements.size() - 1);
                nextCursor = cursorUtil.encodeCursor(
                        lastAgreement.getCreateDate(),
                        lastAgreement.getCreateTime(),
                        lastAgreement.getId());
            }

            Agreement firstAgreement = agreements.get(0);
            prevCursor = cursorUtil.encodeCursor(
                    firstAgreement.getCreateDate(),
                    firstAgreement.getCreateTime(),
                    firstAgreement.getId());
        }

        PageResponse.PageMetadata metadata = PageResponse.PageMetadata.builder()
                .nextCursor(nextCursor)
                .prevCursor(prevCursor)
                .hasNext(hasNext)
                .hasPrev(pageRequest.getCursor() != null)
                .pageSize(limit)
                .returnedCount(agreements.size())
                .build();

        return PageResponse.<Agreement>builder()
                .data(agreements)
                .metadata(metadata)
                .build();
    }
}
