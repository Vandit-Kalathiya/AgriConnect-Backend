package com.agriconnect.Contract.Farming.App.Controller;

import com.agriconnect.Contract.Farming.App.DTO.PageRequest;
import com.agriconnect.Contract.Farming.App.DTO.PageResponse;
import com.agriconnect.Contract.Farming.App.DTO.ResponseData;
import com.agriconnect.Contract.Farming.App.Entity.Agreement;
import com.agriconnect.Contract.Farming.App.Service.AgreementService;
import com.agriconnect.Contract.Farming.App.Service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.util.List;

import static com.razorpay.Utils.bytesToHex;

@RestController
@Validated
public class AgreementController {
    private static final Logger logger = LoggerFactory.getLogger(AgreementController.class);

    private final AgreementService agreementService;

    @Autowired
    public AgreementController(AgreementService agreementService) {
        this.agreementService = agreementService;
    }

    @PostMapping("/upload/{farmerAddress}/{buyerAddress}")
    public ResponseEntity<ResponseData> uploadAgreement(
            @RequestParam("file") MultipartFile file,
            @PathVariable String farmerAddress,
            @PathVariable String buyerAddress) {
        
        logger.info("Uploading agreement for farmer: {} and buyer: {}", farmerAddress, buyerAddress);
        
        try {
            byte[] pdfBytes = file.getBytes();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(pdfBytes);
            String pdfHash = bytesToHex(hashBytes);

            Agreement agreement = agreementService.uploadAgreement(file, pdfHash, "", farmerAddress, buyerAddress);
            logger.info("Agreement added to Database with ID: {}", agreement.getId());

            ResponseData responseData = ResponseData.builder()
                    .downloadURL(agreement.getDownloadUrl())
                    .pdfHash(agreement.getPdfHash())
                    .transactionHash(agreement.getTransactionHash())
                    .fileName(agreement.getFileName())
                    .fileSize(agreement.getSize())
                    .fileType(agreement.getFileType())
                    .build();
            
            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            logger.error("Failed to upload agreement: {}", e.getMessage(), e);
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/download/{pdfHash}")
    public ResponseEntity<?> downloadAgreement(@PathVariable String pdfHash) {
        try {
            Agreement agreement = agreementService.getAgreementByPdfHash(pdfHash);

            if (agreement == null) {
                throw new RuntimeException("Agreement not found for given pdfHash: " + pdfHash);
            }

            return  ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(agreement.getFileType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + agreement.getFileName()
                                    + "\"")
                    .body(new ByteArrayResource(agreement.getData()));
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/download/t/{transactionHash}")
    public ResponseEntity<?> downloadAgreementTx(@PathVariable String transactionHash) {
        try {
            Agreement agreement = agreementService.getAgreementByTransactionHash(transactionHash);

            if (agreement == null) {
                throw new RuntimeException("Agreement not found for given pdfHash: " + transactionHash);
            }

            return  ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(agreement.getFileType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + agreement.getFileName()
                                    + "\"")
                    .body(new ByteArrayResource(agreement.getData()));
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/get/{orderId}")
    public ResponseEntity<Agreement> getAgreementByOrderId(@PathVariable String orderId) {
        logger.debug("Fetching agreement by order ID: {}", orderId);
        return ResponseEntity.ok(agreementService.getAgreementByOrderId(orderId));
    }

    /**
     * Get user's agreements with cursor-based pagination (NEW - Recommended)
     * Query params:
     *   - cursor: Pagination cursor (optional, omit for first page)
     *   - limit: Number of records per page (default: 20, max: 100)
     * 
     * Example: GET /user/agreements/{userId}/paginated?limit=20
     */
    @GetMapping("/user/agreements/{userId}/paginated")
    public ResponseEntity<PageResponse<Agreement>> getAgreementsByUserIdPaginated(
            @PathVariable String userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        
        logger.info("Fetching paginated agreements for user: {}", userId);
        
        PageRequest pageRequest = PageRequest.builder()
                .cursor(cursor)
                .limit(limit)
                .sortDirection("DESC")
                .build();
        
        PageResponse<Agreement> response = agreementService.getAgreementsByAddressPaginated(userId, pageRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's agreements (deprecated - use /user/agreements/{userId}/paginated instead)
     */
    @GetMapping("/user/agreements/{userId}")
    @Deprecated
    public ResponseEntity<List<Agreement>> getAgreementsByUserId(@PathVariable String userId) {
        logger.warn("Using deprecated endpoint - consider using /user/agreements/{userId}/paginated");
        return ResponseEntity.ok(agreementService.getAgreementsByAddress(userId));
    }

    /**
     * Get all agreements with cursor-based pagination (NEW - Recommended)
     * Query params same as above
     */
    @GetMapping("/paginated")
    public ResponseEntity<PageResponse<Agreement>> getAllAgreementsPaginated(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection) {
        
        logger.info("Fetching all paginated agreements");
        
        PageRequest pageRequest = PageRequest.builder()
                .cursor(cursor)
                .limit(limit)
                .sortDirection(sortDirection)
                .build();
        
        PageResponse<Agreement> response = agreementService.getAgreementsPaginated(pageRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all agreements (deprecated - use /paginated instead)
     */
    @GetMapping("/all")
    @Deprecated
    public ResponseEntity<List<Agreement>> getAllAgreements() {
        logger.warn("Using deprecated endpoint /all - consider using /paginated for better performance");
        return ResponseEntity.ok(agreementService.getAllAgreements());
    }
}
