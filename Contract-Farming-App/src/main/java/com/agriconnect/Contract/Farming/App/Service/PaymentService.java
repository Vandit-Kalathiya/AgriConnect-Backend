package com.agriconnect.Contract.Farming.App.Service;

import com.agriconnect.Contract.Farming.App.Entity.Order;
import com.agriconnect.Contract.Farming.App.Repository.OrderRepository;
import com.agriconnect.Contract.Farming.App.kafka.NotificationEventPublisher;
import com.agriconnect.notification.avro.Priority;
import com.razorpay.RazorpayClient;
import jakarta.persistence.EntityNotFoundException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.currency}")
    private String currency;

    @Value("${notification.topics.contract}")
    private String contractTopic;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private NotificationEventPublisher notificationEventPublisher;

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

        try {
            notificationEventPublisher.publish(contractTopic,
                notificationEventPublisher.buildEvent("CONTRACT_PAYMENT_INITIATED",
                    order.getBuyerAddress(), "contract.payment.initiated",
                    List.of("IN_APP"),
                    Map.of("orderId", orderId, "razorpayOrderId", razorpayOrderId,
                           "amount", String.valueOf(amount)),
                    Priority.NORMAL, orderId + "-pay-init", null, null));
        } catch (Exception ex) {
            logger.warn("[NOTIFY] CONTRACT_PAYMENT_INITIATED failed for order={}: {}", orderId, ex.getMessage());
        }

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

        try {
            Map<String, String> p = Map.of("orderId", orderId, "trackingNumber", trackingNumber,
                                           "confirmedAt", Instant.now().toString());
            notificationEventPublisher.publish(contractTopic,
                notificationEventPublisher.buildEvent("CONTRACT_DELIVERY_CONFIRMED",
                    order.getFarmerAddress(), "contract.delivery.confirmed",
                    List.of("EMAIL", "IN_APP"), p, Priority.HIGH, orderId + "-dlv", null, null));
            notificationEventPublisher.publish(contractTopic,
                notificationEventPublisher.buildEvent("CONTRACT_DELIVERY_CONFIRMED",
                    order.getBuyerAddress(), "contract.delivery.confirmed",
                    List.of("IN_APP"), p, Priority.HIGH, orderId + "-dlv-buyer", null, null));
        } catch (Exception ex) {
            logger.warn("[NOTIFY] CONTRACT_DELIVERY_CONFIRMED failed for order={}: {}", orderId, ex.getMessage());
        }
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

            try {
                notificationEventPublisher.publish(contractTopic,
                    notificationEventPublisher.buildEvent("CONTRACT_PAYMENT_RELEASED",
                        order.getFarmerAddress(), "contract.payment.released",
                        List.of("EMAIL", "IN_APP"),
                        Map.of("orderId", order.getId(), "amount", String.valueOf(order.getAmount()),
                               "releasedAt", Instant.now().toString()),
                        Priority.CRITICAL, order.getId() + "-released", null, null));
            } catch (Exception ex) {
                logger.warn("[NOTIFY] CONTRACT_PAYMENT_RELEASED failed for order={}: {}", order.getId(), ex.getMessage());
            }
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

        try {
            Map<String, String> p = Map.of("orderId", orderId, "returnTracking", returnTrackingNumber,
                                           "actionAt", Instant.now().toString());
            notificationEventPublisher.publish(contractTopic,
                notificationEventPublisher.buildEvent("CONTRACT_RETURN_REQUESTED",
                    order.getFarmerAddress(), "contract.return.requested",
                    List.of("EMAIL", "IN_APP"), p, Priority.HIGH, orderId + "-ret-req", null, null));
            notificationEventPublisher.publish(contractTopic,
                notificationEventPublisher.buildEvent("CONTRACT_RETURN_REQUESTED",
                    order.getBuyerAddress(), "contract.return.requested",
                    List.of("IN_APP"), p, Priority.HIGH, orderId + "-ret-req-buyer", null, null));
        } catch (Exception ex) {
            logger.warn("[NOTIFY] CONTRACT_RETURN_REQUESTED failed for order={}: {}", orderId, ex.getMessage());
        }
    }

    public void confirmReturn(String orderId) throws Exception {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (!"return_requested".equals(order.getStatus())) {
            throw new Exception("Return not requested for order");
        }
        order.setStatus("return_confirmed");
        orderRepository.save(order);
        logger.info("Return confirmed for order: {}", order.getRazorpayOrderId());

        try {
            notificationEventPublisher.publish(contractTopic,
                notificationEventPublisher.buildEvent("CONTRACT_RETURN_CONFIRMED",
                    order.getBuyerAddress(), "contract.return.confirmed",
                    List.of("EMAIL", "IN_APP"),
                    Map.of("orderId", orderId, "actionAt", Instant.now().toString()),
                    Priority.HIGH, orderId + "-ret-conf", null, null));
        } catch (Exception ex) {
            logger.warn("[NOTIFY] CONTRACT_RETURN_CONFIRMED failed for order={}: {}", orderId, ex.getMessage());
        }
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

            try {
                Map<String, String> p = Map.of("orderId", order.getId(), "refundId", (String) refund.get("id"),
                                               "amount", String.valueOf(order.getAmount()),
                                               "actionAt", Instant.now().toString());
                notificationEventPublisher.publish(contractTopic,
                    notificationEventPublisher.buildEvent("CONTRACT_DELIVERY_REJECTED",
                        order.getBuyerAddress(), "contract.delivery.rejected",
                        List.of("EMAIL", "IN_APP"), p, Priority.CRITICAL, order.getId() + "-refund", null, null));
                notificationEventPublisher.publish(contractTopic,
                    notificationEventPublisher.buildEvent("CONTRACT_DELIVERY_REJECTED",
                        order.getFarmerAddress(), "contract.delivery.rejected",
                        List.of("EMAIL", "IN_APP"), p, Priority.CRITICAL, order.getId() + "-refund-farmer", null, null));
            } catch (Exception ex) {
                logger.warn("[NOTIFY] CONTRACT_DELIVERY_REJECTED failed for order={}: {}", order.getId(), ex.getMessage());
            }
        } else if ("captured".equals(payment.get("status"))) {
            throw new Exception("Payment already captured, cannot refund directly");
        } else {
            throw new Exception("Payment not in refundable state");
        }
    }
}
