package com.agriconnect.Contract.Farming.App.Controller;

import com.agriconnect.Contract.Farming.App.DTO.PaymentCreateOrderRequest;
//import com.agriconnect.Contract.Farming.App.Service.AgreementBlockChainService;
import com.agriconnect.Contract.Farming.App.Entity.Order;
import com.agriconnect.Contract.Farming.App.Repository.OrderRepository;
import com.agriconnect.Contract.Farming.App.Service.OrderService;
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

//    @Autowired
//    private Ag    reementBlockChainService agreementBlockChainService;

    @Autowired
    private OrderRepository orderRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private OrderService orderService;

    @PostMapping(value = "/create-order", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createOrder(PaymentCreateOrderRequest paymentCreateOrderRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
//            byte[] pdfBytes = file.getBytes();
//            MessageDigest digest = MessageDigest.getInstance("SHA-256");
//            byte[] hashBytes = digest.digest(pdfBytes);
//            String pdfHash = bytesToHex(hashBytes);

            String farmerAddress = paymentCreateOrderRequest.getFarmerAddress();
            String buyerAddress = paymentCreateOrderRequest.getBuyerAddress();
            Long amount = paymentCreateOrderRequest.getAmount();
            String orderId = paymentCreateOrderRequest.getOrderId();


            String razorpayOrderId = paymentService.createOrder(farmerAddress, buyerAddress, amount, orderId);

            response.put("success", true);
            response.put("razorpayOrderId", razorpayOrderId);
//            response.put("pdfHash", pdfHash);
            response.put("keyId", razorpayKeyId);
            response.put("currency", "INR");
            response.put("amount", String.valueOf(amount * 100));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to create order: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping(value = "/payment-callback", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> paymentCallback(
            @RequestParam("razorpay_order_id") String orderId,
            @RequestParam("razorpay_payment_id") String paymentId,
            @RequestParam("razorpay_signature") String signature) {
        Map<String, Object> response = new HashMap<>();
        try {
            String payload = orderId + "|" + paymentId;
            boolean isValid = Utils.verifySignature(payload, signature, razorpayKeySecret);

            if (isValid) {
                Order order = orderRepository.findByRazorpayOrderId(orderId);
                if (order != null && "created".equals(order.getStatus())) {
                    order.setRazorpayPaymentId(paymentId);
                    order.setRazorpaySignature(signature);
                    order.setStatus("paid_pending_delivery");
                    orderRepository.save(order);
//                    paymentService.addPaymentDetails(order.getId(), paymentId, order.getAmount());

                    response.put("success", true);
                    response.put("message", "Payment authorized, awaiting delivery");
                    response.put("data", order);
                    return ResponseEntity.ok(response);
                } else {
                    response.put("success", false);
                    response.put("message", "Order not found or already processed");
                    return ResponseEntity.status(404).body(response);
                }
            } else {
                response.put("success", false);
                response.put("message", "Payment verification failed");
                return ResponseEntity.status(400).body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error in payment callback: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping(value = "/confirm-delivery/{orderId}/{trackingNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> confirmDelivery(
            @PathVariable String orderId,
            @PathVariable String trackingNumber) {
        Map<String, Object> response = new HashMap<>();
        try {
            paymentService.confirmDelivery(orderId, trackingNumber);
            response.put("success", true);
            response.put("message", "Delivery confirmed, awaiting buyer verification");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to confirm delivery: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping(value = "/verify-delivery/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> verifyDelivery(
            @PathVariable("orderId") String orderId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Order order = paymentService.verifyAndReleasePayment(orderId);
            response.put("success", true);
            response.put("message", "Delivery verified, payment released to farmer");
            response.put("data", order);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to verify delivery: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping(value = "/request-return/{orderId}/{returnTrackingNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> requestReturn(
            @PathVariable String orderId,
            @PathVariable String returnTrackingNumber) {
        Map<String, Object> response = new HashMap<>();
        try {
            paymentService.requestReturn(orderId, returnTrackingNumber);
            response.put("success", true);
            response.put("message", "Return requested, awaiting farmer confirmation");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to request return: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping(value = "/confirm-return/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> confirmReturn(
            @PathVariable("orderId") String orderId) {
        Map<String, Object> response = new HashMap<>();
        try {
            paymentService.confirmReturn(orderId);
            response.put("success", true);
            response.put("message", "Return confirmed by farmer, buyer can now request refund");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to confirm return: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping(value = "/reject-delivery/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> rejectDelivery(
            @PathVariable("orderId") String orderId) {
        Map<String, Object> response = new HashMap<>();
        try {
            paymentService.rejectAndRefundPayment(orderId);
            response.put("success", true);
            response.put("message", "Delivery rejected, payment refunded to buyer");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to reject delivery: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Get All Payments of a Buyer by userId
    @GetMapping("/buyer/{buyerAddress}")
    public ResponseEntity<?> getPaymentsByBuyerAddress(@PathVariable String buyerAddress) {
        try {
            return ResponseEntity.ok(orderService.getAllCompletedOrdersByBuyer(buyerAddress));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching payments: " + e.getMessage());
        }
    }

    // Get All Payments of a Farmer by userId
    @GetMapping("/farmer/{farmerAddress}")
    public ResponseEntity<?> getPaymentsByFarmerAddress(@PathVariable String farmerAddress) {
        try {
            return ResponseEntity.ok(orderService.getAllCompletedOrdersByFarmer(farmerAddress));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching payments: " + e.getMessage());
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