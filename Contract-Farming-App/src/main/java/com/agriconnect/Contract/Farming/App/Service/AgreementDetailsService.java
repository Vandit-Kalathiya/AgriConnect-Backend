package com.agriconnect.Contract.Farming.App.Service;


import com.agriconnect.Contract.Farming.App.Entity.AgreementDetails.AgreementDetails;
import com.agriconnect.Contract.Farming.App.Entity.AgreementDetails.TermCondition;
import com.agriconnect.Contract.Farming.App.Repository.AgreementDetailsRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class AgreementDetailsService {

    private final AgreementDetailsRepository agreementDetailsRepository;

    public AgreementDetailsService(AgreementDetailsRepository agreementDetailsRepository) {
        this.agreementDetailsRepository = agreementDetailsRepository;
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

            // Save and return the persisted entity
            return agreementDetailsRepository.save(agreementToSave);

        } catch (Exception e) {
            throw new RuntimeException("Failed to save agreement details: " + e.getMessage(), e);
        }
    }

    public AgreementDetails getAgreementDetailsById(String id) {
        return agreementDetailsRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agreement details not found with id: " + id));
    }

    public List<AgreementDetails> getAllAgreementDetails() {
        List<AgreementDetails> agreements = agreementDetailsRepository.findAll();
        if (agreements.isEmpty()) {
            return new ArrayList<>(); // Return empty list instead of null
        }
        return agreements;
    }
}
