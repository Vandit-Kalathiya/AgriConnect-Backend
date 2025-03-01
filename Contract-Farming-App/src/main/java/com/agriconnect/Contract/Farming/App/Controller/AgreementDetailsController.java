package com.agriconnect.Contract.Farming.App.Controller;

import com.agriconnect.Contract.Farming.App.Entity.AgreementDetails.AgreementDetails;
import com.agriconnect.Contract.Farming.App.Service.AgreementDetailsService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/agreements")
public class AgreementDetailsController {

    private final AgreementDetailsService agreementDetailsService;

    @Autowired
    public AgreementDetailsController(AgreementDetailsService agreementDetailsService) {
        this.agreementDetailsService = agreementDetailsService;
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveAgreementDetails(@RequestPart("farmerSignature") MultipartFile farmerSignature,
                                                  @RequestPart("buyerSignature") MultipartFile buyerSignature,
                                                  @RequestPart("agreementDetails") AgreementDetails agreementDetails) {
        try {
            AgreementDetails savedAgreement = agreementDetailsService.saveAgreementDetails(farmerSignature,buyerSignature,agreementDetails);
            return ResponseEntity.ok(savedAgreement);
        } catch (RuntimeException e) {
            return new ResponseEntity<>("Failed to save agreement: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Unexpected error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

        @GetMapping("/get/{id}")
        public ResponseEntity<?> getAgreementDetailsById(@PathVariable String id) {
            try {
                AgreementDetails agreement = agreementDetailsService.getAgreementDetailsById(id);
                return ResponseEntity.ok(agreement);
            } catch (EntityNotFoundException e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
            } catch (Exception e) {
                return new ResponseEntity<>("Unexpected error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

    @GetMapping("/all")
    public ResponseEntity<?> getAllAgreementDetails() {
        try {
            List<AgreementDetails> agreements = agreementDetailsService.getAllAgreementDetails();
            return ResponseEntity.ok(agreements);
        } catch (Exception e) {
            return new ResponseEntity<>("Unexpected error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}