package com.agriconnect.Contract.Farming.App.Controller;

import com.agriconnect.Contract.Farming.App.AgreementRegistry.AgreementRegistry;
import com.agriconnect.Contract.Farming.App.DTO.UploadResponse;
import com.agriconnect.Contract.Farming.App.Entity.Order;
import com.agriconnect.Contract.Farming.App.Repository.OrderRepository;
import com.agriconnect.Contract.Farming.App.Service.AgreementBlockChainService;
import com.agriconnect.Contract.Farming.App.Service.AgreementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/agreements")
public class AgreementBlockChainController {

    private static final Logger logger = LoggerFactory.getLogger(AgreementBlockChainController.class);

    @Value("${razorpay.currency}")
    private String currency;

    @Autowired
    private AgreementBlockChainService agreementBlockChainService;

    @Autowired
    private AgreementService agreementService;

    @Autowired
    private OrderRepository orderRepository;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadAgreement(
//            @RequestParam("file") MultipartFile file,
            @RequestParam("farmerAddress") String farmerAddress,
            @RequestParam("buyerAddress") String buyerAddress,
            @RequestParam("orderId") String orderId
//            @RequestParam("amount") Long amount
    ) {
        try {
//            byte[] pdfBytes = file.getBytes();
//            MessageDigest digest = MessageDigest.getInstance("SHA-256");
//            byte[] hashBytes = digest.digest(pdfBytes);
//            String pdfHash = bytesToHex(hashBytes);

            String txHash = agreementBlockChainService.addAgreement(orderId, farmerAddress, buyerAddress);
            logger.info("Successfully added agreement to blockchain with txHash: {}", txHash);

//            Order order = new Order();
//            order.setPdfHash(pdfHash);
//            order.setFarmerAddress(farmerAddress);
//            order.setBuyerAddress(buyerAddress);
//            order.setCurrency(currency);
//            order.setStatus("Pending");
//            order.setCreatedDate(LocalDate.now());
//            order.setCreatedTime(LocalTime.now());
//            order.setAmount(amount);
//            Order savedOrder = orderRepository.save(order);
//
//            agreementService.uploadAgreement(file, txHash, pdfHash, orderId, farmerAddress, buyerAddress);
//            logger.info("Successfully saved agreement to database");

            return ResponseEntity.ok(UploadResponse.builder()
                    .pdfHash(orderId)
                    .txHash(txHash)
                    .downloadUrl("") // Add logic for download URL if needed
                    .build());
        } catch (Exception e) {
            logger.error("Error uploading agreement: {}", e.getMessage());
            return ResponseEntity.status(500).body("Error uploading agreement: " + e.getMessage());
        }
    }

    @GetMapping("/hash/{orderId}")
    public ResponseEntity<?> getAgreement(@PathVariable String orderId) {
        try {
            AgreementRegistry.Agreement agreement = agreementBlockChainService.getAgreement(orderId);
            return ResponseEntity.ok(agreement);
        } catch (Exception e) {
            logger.error("Error fetching agreement: {}", e.getMessage());
            return ResponseEntity.status(500).body("Error fetching agreement: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllAgreements() {
        try {
            List<AgreementRegistry.Agreement> agreements = agreementBlockChainService.getAllAgreements();
            return ResponseEntity.ok(agreements);
        } catch (Exception e) {
            logger.error("Error fetching all agreements: {}", e.getMessage());
            return ResponseEntity.status(500).body("Error fetching all agreements: " + e.getMessage());
        }
    }

    @GetMapping("/count")
    public ResponseEntity<?> getTotalAgreements() {
        try {
            String count = agreementBlockChainService.getTotalAgreements();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            logger.error("Error fetching total agreements: {}", e.getMessage());
            return ResponseEntity.status(500).body("Error fetching total agreements: " + e.getMessage());
        }
    }

    @PutMapping("/status/{orderId}")
    public ResponseEntity<?> updateStatus(
            @PathVariable String orderId,
            @RequestParam("status") int status
    ) {
        try {
            String txHash = agreementBlockChainService.updateAgreementStatus(orderId, status);
            return ResponseEntity.ok(txHash);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating status: {}", e.getMessage());
            return ResponseEntity.status(500).body("Error updating status: " + e.getMessage());
        }
    }

    @DeleteMapping("/hash/{orderId}")
    public ResponseEntity<?> deleteAgreement(@PathVariable String orderId) {
        try {
            String txHash = agreementBlockChainService.deleteAgreement(orderId);
            return ResponseEntity.ok(txHash);
        } catch (Exception e) {
            logger.error("Error deleting agreement: {}", e.getMessage());
            return ResponseEntity.status(500).body("Error deleting agreement: " + e.getMessage());
        }
    }

    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAllAgreements() {
        try {
            String txHash = agreementBlockChainService.deleteAllAgreements();
            return ResponseEntity.ok(txHash);
        } catch (Exception e) {
            logger.error("Error deleting all agreements: {}", e.getMessage());
            return ResponseEntity.status(500).body("Error deleting all agreements: " + e.getMessage());
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}