package com.agriconnect.Contract.Farming.App.Controller;

//import com.agriconnect.Contract.Farming.App.AgreementRegistry.AgreementRegistry;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.security.MessageDigest;
import java.util.List;

import static com.razorpay.Utils.bytesToHex;

@RestController
public class AgreementController {
    private static final Logger logger = LoggerFactory.getLogger(AgreementController.class);

    @Autowired
    private AgreementService agreementService;

//    @Autowired
//    private AgreementRegistry agreementRegistry;

    @PostMapping("/upload/{farmerAddress}/{buyerAddress}")
    public ResponseEntity<?> uploadAgreement(@RequestParam("file") MultipartFile file,
                                             @PathVariable String farmerAddress,
                                             @PathVariable String buyerAddress) {
        try {
            byte[] pdfBytes = file.getBytes();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(pdfBytes);
            String pdfHash = bytesToHex(hashBytes);
//            TransactionReceipt receipt = agreementRegistry.addAgreement(pdfHash, farmerAddress, buyerAddress).send();
//            logger.info("Agreement added to blockchain. Tx Hash: {}", receipt.getTransactionHash());

            Agreement agreement = agreementService.uploadAgreement(file, pdfHash,"",farmerAddress,buyerAddress);
            logger.info("Agreement added to Database.");

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
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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
    public Agreement getAgreementByOrderId(@PathVariable String orderId) {
        return agreementService.getAgreementByOrderId(orderId);
    }

    @GetMapping("/user/agreements/{userId}")
    public ResponseEntity<List<Agreement>> getAgreementsByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(agreementService.getAgreementsByAddress(userId));
    }

    // Get All Agreements
    @GetMapping("/all")
    public ResponseEntity<List<Agreement>> getAllAgreements() {
        return ResponseEntity.ok(agreementService.getAllAgreements());
    }
}
