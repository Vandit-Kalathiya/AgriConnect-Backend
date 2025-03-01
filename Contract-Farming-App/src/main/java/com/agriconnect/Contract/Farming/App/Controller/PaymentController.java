package com.agriconnect.Contract.Farming.App.Controller;

import com.agriconnect.Contract.Farming.App.Service.AgreementBlockChainService;
import com.agriconnect.Contract.Farming.App.Entity.Order;
import com.agriconnect.Contract.Farming.App.Repository.OrderRepository;
import com.agriconnect.Contract.Farming.App.Service.PaymentService;
import com.razorpay.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private AgreementBlockChainService agreementBlockChainService;

    @Autowired
    private OrderRepository orderRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;
    @Autowired
    private PaymentService paymentService;

    @PostMapping(value = "/create-order", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> createOrder(
            @RequestParam("file") MultipartFile file,
            @RequestParam("farmerAddress") String farmerAddress,
            @RequestParam("buyerAddress") String buyerAddress,
            @RequestParam("orderId") String orderId,
            @RequestParam("amount") Long amount) throws Exception {
        byte[] pdfBytes = file.getBytes();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(pdfBytes);
        String pdfHash = bytesToHex(hashBytes);

        String razorpayOrderId = paymentService.createOrder(file, pdfHash, farmerAddress, buyerAddress, amount, orderId);

        Map<String, String> response = new HashMap<>();
        response.put("razorpayOrderId", razorpayOrderId);
        response.put("pdfHash", pdfHash);
        response.put("keyId", razorpayKeyId);
        response.put("currency", "INR");
        response.put("amount", String.valueOf(amount*100));// changed
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/payment-callback", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> paymentCallback(
            @RequestParam("razorpay_order_id") String orderId,
            @RequestParam("razorpay_payment_id") String paymentId,
            @RequestParam("razorpay_signature") String signature) throws Exception {
        String payload = orderId + "|" + paymentId;
        boolean isValid = Utils.verifySignature(payload, signature, razorpayKeySecret);

        if (isValid) {
            Order order = orderRepository.findByRazorpayOrderId(orderId);
            if (order != null && "created".equals(order.getStatus())) {
                order.setRazorpayPaymentId(paymentId);
                order.setRazorpaySignature(signature);
                order.setStatus("paid_pending_delivery");
                orderRepository.save(order);
                paymentService.addPaymentDetails(order.getPdfHash(), paymentId, order.getAmount());
                return ResponseEntity.ok("Payment authorized, awaiting delivery");
            } else {
                return ResponseEntity.status(404).body("Order not found or already processed");
            }
        } else {
            return ResponseEntity.status(400).body("Payment verification failed");
        }
    }

    @PostMapping(value = "/confirm-delivery/{pdfHash}/{trackingNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> confirmDelivery(
            @PathVariable("pdfHash") String pdfHash,
            @PathVariable("trackingNumber") String trackingNumber) throws Exception {
        paymentService.confirmDelivery(pdfHash, trackingNumber);
        return ResponseEntity.ok("Delivery confirmed, awaiting buyer verification");
    }

    @PostMapping(value = "/verify-delivery/{pdfHash}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> verifyDelivery(
            @PathVariable("pdfHash") String pdfHash) throws Exception {
        paymentService.verifyAndReleasePayment(pdfHash);
        return ResponseEntity.ok("Delivery verified, payment released to farmer");
    }

    @PostMapping(value = "/request-return", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> requestReturn(
            @RequestParam("pdfHash") String pdfHash,
            @RequestParam("returnTrackingNumber") String returnTrackingNumber) throws Exception {
        paymentService.requestReturn(pdfHash, returnTrackingNumber);
        return ResponseEntity.ok("Return requested, awaiting farmer confirmation");
    }

    @PostMapping(value = "/confirm-return", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> confirmReturn(
            @RequestParam("pdfHash") String pdfHash) throws Exception {
        paymentService.confirmReturn(pdfHash);
        return ResponseEntity.ok("Return confirmed by farmer, buyer can now request refund");
    }

    @PostMapping(value = "/reject-delivery", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> rejectDelivery(
            @RequestParam("pdfHash") String pdfHash) throws Exception {
        try {
            paymentService.rejectAndRefundPayment(pdfHash);
            return ResponseEntity.ok("Delivery rejected, payment refunded to buyer");
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error rejecting delivery: " + e.getMessage());
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
