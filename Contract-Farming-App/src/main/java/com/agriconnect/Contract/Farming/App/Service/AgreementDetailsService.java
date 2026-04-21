package com.agriconnect.Contract.Farming.App.Service;

import com.agriconnect.Contract.Farming.App.Entity.AgreementDetails.AgreementDetails;
import com.agriconnect.Contract.Farming.App.Entity.AgreementDetails.TermCondition;
import com.agriconnect.Contract.Farming.App.Repository.AgreementDetailsRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class AgreementDetailsService {

    private final AgreementDetailsRepository agreementDetailsRepository;
    private final CacheService cacheService;

    private static final Duration DETAILS_TTL = Duration.ofHours(12);
    private static final String ALL_DETAILS_KEY = "agreement-details:all";

    public AgreementDetailsService(AgreementDetailsRepository agreementDetailsRepository,
            CacheService cacheService) {
        this.agreementDetailsRepository = agreementDetailsRepository;
        this.cacheService = cacheService;
    }

    public AgreementDetails saveAgreementDetails(
            MultipartFile farmerSignature,
            MultipartFile buyerSignature,
            AgreementDetails agreementDetails) {
        try {
            // Create a new AgreementDetails instance
            AgreementDetails agreementToSave = new AgreementDetails();

            // Copy basic embedded objects
            agreementToSave.setFarmerInfo(agreementDetails.getFarmerInfo());
            agreementToSave.setBuyerInfo(agreementDetails.getBuyerInfo());
            agreementToSave.setCropDetails(agreementDetails.getCropDetails());
            agreementToSave.setDeliveryTerms(agreementDetails.getDeliveryTerms());
            agreementToSave.setPaymentTerms(agreementDetails.getPaymentTerms());
            agreementToSave.setAdditionalNotes(agreementDetails.getAdditionalNotes());

            // Set signatures
            if (farmerSignature != null && !farmerSignature.isEmpty()) {
                agreementToSave.getFarmerInfo().setFarmerSignature(farmerSignature.getBytes());
            }
            if (buyerSignature != null && !buyerSignature.isEmpty()) {
                agreementToSave.getBuyerInfo().setBuyerSignature(buyerSignature.getBytes());
            }

            // Handle TermConditions properly
            if (agreementDetails.getTermConditions() != null && !agreementDetails.getTermConditions().isEmpty()) {
                List<TermCondition> terms = new ArrayList<>();
                // Deep copy to avoid reference issues
                for (TermCondition term : agreementDetails.getTermConditions()) {
                    TermCondition newTerm = new TermCondition();
                    newTerm.setContent(term.getContent());
                    newTerm.setTitle(term.getTitle());
                    newTerm.setTId(term.getTId());
                    terms.add(newTerm);
                }
                agreementToSave.setTermConditions(terms);
            } else {
                // Ensure termConditions is initialized even if input is null
                agreementToSave.setTermConditions(new ArrayList<>());
            }

            AgreementDetails saved = agreementDetailsRepository.save(agreementToSave);
            cacheService.evict(ALL_DETAILS_KEY);
            return saved;

        } catch (Exception e) {
            throw new RuntimeException("Failed to save agreement details: " + e.getMessage(), e);
        }
    }

    public AgreementDetails getAgreementDetailsById(String id) {
        String cacheKey = "agreement-details:" + id;
        return cacheService.get(cacheKey, AgreementDetails.class).orElseGet(() -> {
            AgreementDetails details = agreementDetailsRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Agreement details not found with id: " + id));
            cacheService.save(cacheKey, details, DETAILS_TTL);
            return details;
        });
    }

    public List<AgreementDetails> getAllAgreementDetails() {
        return cacheService.get(ALL_DETAILS_KEY, List.class).orElseGet(() -> {
            List<AgreementDetails> agreements = agreementDetailsRepository.findAll();
            List<AgreementDetails> result = agreements.isEmpty() ? new ArrayList<>() : agreements;
            cacheService.save(ALL_DETAILS_KEY, result, Duration.ofHours(1));
            return result;
        });
    }
}
