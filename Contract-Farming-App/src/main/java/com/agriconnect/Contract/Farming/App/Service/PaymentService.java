package com.agriconnect.Contract.Farming.App.Service;

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

    public String createOrder(String farmerAddress, String buyerAddress, Long amount, String orderId) throws Exception {
        RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount * 100); // in paise
        orderRequest.put("currency", currency);
        orderRequest.put("payment_capture", 0); // manual capture for escrow-like behaviour

        com.razorpay.Order razorpayOrder = razorpay.orders.create(orderRequest);
        String razorpayOrderId = razorpayOrder.get("id");

        Order order = orderRepository.findById(orderId).get();
        order.setAmount(amount);
        order.setRazorpayOrderId(razorpayOrderId);
        orderRepository.save(order);
        return razorpayOrderId;
    }

    public void confirmDelivery(String orderId, String trackingNumber) throws Exception {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (!"paid_pending_delivery".equals(order.getStatus())) {
            throw new Exception("Order not in payable state");
        }
        order.setTrackingNumber(trackingNumber);
        order.setStatus("delivered");
        orderRepository.save(order);
        logger.info("Delivery confirmed for order: {}", order.getRazorpayOrderId());
    }

    public Order verifyAndReleasePayment(String orderId) throws Exception {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (!"delivered".equals(order.getStatus())) {
            throw new Exception("Order not delivered");
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
        return order;
    }

    public void requestReturn(String orderId, String returnTrackingNumber) throws Exception {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (!"delivered".equals(order.getStatus())) {
            throw new Exception("Order not delivered");
        }
        order.setReturnTrackingNumber(returnTrackingNumber);
        order.setStatus("return_requested");
        orderRepository.save(order);
        logger.info("Return requested for order: {}", order.getRazorpayOrderId());
    }

    public void confirmReturn(String orderId) throws Exception {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (!"return_requested".equals(order.getStatus())) {
            throw new Exception("Return not requested for order");
        }
        order.setStatus("return_confirmed");
        orderRepository.save(order);
        logger.info("Return confirmed for order: {}", order.getRazorpayOrderId());
    }

    public void rejectAndRefundPayment(String orderId) throws Exception {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (!"return_confirmed".equals(order.getStatus())) {
            throw new Exception("Return not confirmed for order");
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
            logger.info("Payment refunded for order: {}. Refund ID: {}", order.getRazorpayOrderId(), refund.get("id"));
        } else if ("captured".equals(payment.get("status"))) {
            throw new Exception("Payment already captured, cannot refund directly");
        } else {
            throw new Exception("Payment not in refundable state");
        }
    }
}
