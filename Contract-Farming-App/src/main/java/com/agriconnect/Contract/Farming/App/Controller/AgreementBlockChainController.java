package com.agriconnect.Contract.Farming.App.Controller;

import com.agriconnect.Contract.Farming.App.AgreementRegistry.AgreementRegistry;
import com.agriconnect.Contract.Farming.App.DTO.UploadResponse;
import com.agriconnect.Contract.Farming.App.Entity.Agreement;
import com.agriconnect.Contract.Farming.App.Entity.Order;
import com.agriconnect.Contract.Farming.App.Repository.OrderRepository;
import com.agriconnect.Contract.Farming.App.Service.AgreementBlockChainService;
import com.agriconnect.Contract.Farming.App.Service.AgreementService;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AgreementBlockChainController {

    Logger logger = LoggerFactory.getLogger(this.getClass());

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
            @RequestParam("file") MultipartFile file,
            @RequestParam("farmerAddress") String farmerAddress,
            @RequestParam("buyerAddress") String buyerAddress,
            @RequestParam("listingId") String listingId,
            @RequestParam("amount") Long amount
    ) throws Exception {
        byte[] pdfBytes = file.getBytes();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(pdfBytes);
        String pdfHash = bytesToHex(hashBytes);

        String txHash = agreementBlockChainService.addAgreement(pdfHash, farmerAddress, buyerAddress);

        logger.info("Successfully added agreement to blockchain");


        Order order = new Order();
        order.setPdfHash(pdfHash);
        order.setFarmerAddress(farmerAddress);
        order.setBuyerAddress(buyerAddress);
        order.setCurrency(currency);
        order.setStatus("created");
        order.setCreatedDate(LocalDate.now());
        order.setCreatedTime(LocalTime.now());
        order.setListingId(listingId);
        order.setAmount(amount);
        Order savedOrder = orderRepository.save(order);

        Agreement agreement = agreementService.uploadAgreement(file, txHash, pdfHash, savedOrder.getId(),farmerAddress,buyerAddress);

        logger.info("Successfully saved agreement to database");

        return ResponseEntity.ok(UploadResponse.builder().pdfHash(pdfHash).txHash(txHash).downloadUrl(agreement.getDownloadUrl()).build());
    }

    @GetMapping("/hash/{pdfHash}")
    public ResponseEntity<AgreementRegistry.Agreement> getAgreement(@PathVariable String pdfHash) throws Exception {
        try {
            AgreementRegistry.Agreement agreement = agreementBlockChainService.getAgreement(pdfHash);
            return ResponseEntity.ok(agreement);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<AgreementRegistry.Agreement>> getAllAgreements() throws Exception {
        List<AgreementRegistry.Agreement> hashes = agreementBlockChainService.getAllAgreements();
        return ResponseEntity.ok(hashes);
    }

    @GetMapping("/count")
    public ResponseEntity<BigInteger> getTotalAgreements() throws Exception {
        BigInteger count = agreementBlockChainService.getTotalAgreements();
        return ResponseEntity.ok(count);
    }

    @PutMapping("/status/{pdfHash}")
    public ResponseEntity<String> updateStatus(
            @PathVariable String pdfHash,
            @RequestParam("status") int status
    ) throws Exception {
        if (status < 0 || status > 3) {
            return ResponseEntity.badRequest().body("Status must be 0-3");
        }
        String txHash = agreementBlockChainService.updateAgreementStatus(pdfHash, status);
        return ResponseEntity.ok(txHash);
    }

    @DeleteMapping("/hash/{pdfHash}")
    public ResponseEntity<String> deleteAgreement(@PathVariable String pdfHash) throws Exception {
        try {
            String txHash = agreementService.deleteAgreement(pdfHash);
            return ResponseEntity.ok(txHash);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    // New endpoint: Delete all agreements
    @DeleteMapping("/all")
    public ResponseEntity<String> deleteAllAgreements() throws Exception {
        try {
            String txHash = agreementService.deleteAllAgreements();
            return ResponseEntity.ok(txHash);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
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
