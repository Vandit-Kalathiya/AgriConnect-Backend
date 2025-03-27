package com.agriconnect.Contract.Farming.App.Service;

import com.agriconnect.Contract.Farming.App.AgreementRegistry.AgreementRegistry;
import com.agriconnect.Contract.Farming.App.Entity.Order;
import com.agriconnect.Contract.Farming.App.Repository.OrderRepository;
import com.razorpay.RazorpayClient;
import jakarta.persistence.EntityNotFoundException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.currency}")
    private String currency;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AgreementRegistry agreementRegistry;

    @Autowired
    private AgreementService agreementService;

    public void addPaymentDetails(String orderId, String paymentId, Long amount) throws Exception {
        TransactionReceipt receipt = agreementRegistry.addPaymentDetails(orderId, paymentId, BigInteger.valueOf(amount)).send();
        logger.info("Payment details added to blockchain for hash: {}. Tx Hash: {}", orderId, receipt.getTransactionHash());
    }

    public String createOrder(String farmerAddress, String buyerAddress, Long amount, String orderId) throws Exception {
        // Add agreement to blockchain
        TransactionReceipt receipt = agreementRegistry.addAgreement(orderId, farmerAddress, buyerAddress).send();
        logger.info("Agreement added to blockchain. Tx Hash: {}", receipt.getTransactionHash());
//
//        agreementService.uploadAgreement(file, receipt.getTransactionHash(),pdfHash);
//        logger.info("Agreement added to Database.");

        // Create Razorpay order
        RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount * 100); // In paise
        orderRequest.put("currency", currency);
//        orderRequest.put("receipt", "agr_" + pdfHash.substring(0, 8));
        orderRequest.put("payment_capture", 0); // Manual capture for escrow-like behavior

        com.razorpay.Order razorpayOrder = razorpay.orders.create(orderRequest);
        String razorpayOrderId = razorpayOrder.get("id");

        // Save order
        Order order = orderRepository.findById(orderId).get();
//        order.setPdfHash(pdfHash);
//        order.setFarmerAddress(farmerAddress);
//        order.setBuyerAddress(buyerAddress);
        order.setAmount(amount);
//        order.setCurrency(currency);
        order.setRazorpayOrderId(razorpayOrderId);
//        order.setStatus("created");
//        order.setCreatedDate(LocalDate.now());
//        order.setCreatedTime(LocalTime.now());
        orderRepository.save(order);
        return razorpayOrderId;
    }

    public void confirmDelivery(String orderId, String trackingNumber) throws Exception {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (order == null || !"paid_pending_delivery".equals(order.getStatus())) {
            throw new Exception("Order not found or not in payable state");
        }
        order.setTrackingNumber(trackingNumber);
        order.setStatus("delivered");
        orderRepository.save(order);
        logger.info("Delivery confirmed for order: {}", order.getRazorpayOrderId());
    }

    public void verifyAndReleasePayment(String orderId) throws Exception {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (order == null || !"delivered".equals(order.getStatus())) {
            throw new Exception("Order not found or not delivered");
        }

        RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        com.razorpay.Payment payment = razorpay.payments.fetch(order.getRazorpayPaymentId());
        if ("authorized".equals(payment.get("status"))) {
            JSONObject captureRequest = new JSONObject();
            captureRequest.put("amount", order.getAmount() * 100);
            captureRequest.put("currency", currency);
            razorpay.payments.capture(order.getRazorpayPaymentId(), captureRequest);
            order.setStatus("completed");
            orderRepository.save(order);
            logger.info("Payment captured and released for order: {}", order.getRazorpayOrderId());
        } else {
            throw new Exception("Payment not authorized");
        }
    }

    public void requestReturn(String orderId, String returnTrackingNumber) throws Exception {
        System.out.println(orderId);
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        System.out.println(order.getStatus());
        if (order == null || !"delivered".equals(order.getStatus())) {
            throw new Exception("Order not found or not delivered");
        }
        order.setReturnTrackingNumber(returnTrackingNumber);
        order.setStatus("return_requested");
        orderRepository.save(order);
        agreementRegistry.requestReturn(orderId).send();
        logger.info("Return requested for order: {}", order.getRazorpayOrderId());
    }

    public void confirmReturn(String orderId) throws Exception {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (order == null || !"return_requested".equals(order.getStatus())) {
            throw new Exception("Order not found or return not requested");
        }
        order.setStatus("return_confirmed");
        orderRepository.save(order);
        agreementRegistry.confirmReturn(orderId).send();
        logger.info("Return confirmed for order: {}", order.getRazorpayOrderId());
    }

    public void rejectAndRefundPayment(String orderId) throws Exception {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (order == null || !"return_confirmed".equals(order.getStatus())) {
            throw new Exception("Order not found or not return confirmed");
        }

        RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        com.razorpay.Payment payment = razorpay.payments.fetch(order.getRazorpayPaymentId());
        if ("authorized".equals(payment.get("status"))) {
            JSONObject captureRequest = new JSONObject();
            captureRequest.put("amount", order.getAmount() * 100);
            captureRequest.put("currency", currency);
            razorpay.payments.capture(order.getRazorpayPaymentId(), captureRequest);
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", order.getAmount());
            com.razorpay.Refund refund = razorpay.payments.refund(order.getRazorpayPaymentId(), refundRequest);
            order.setRazorpayRefundId(refund.get("id"));
            order.setStatus("refunded");
            orderRepository.save(order);
            agreementRegistry.recordRefund(orderId, refund.get("id")).send();
            logger.info("Payment refunded for order: {}. Refund ID: {}", order.getRazorpayOrderId(), refund.get("id"));
        } else if ("captured".equals(payment.get("status"))) {
            throw new Exception("Payment already captured, cannot refund directly");
        } else {
            throw new Exception("Payment not in refundable state");
        }
    }
}
