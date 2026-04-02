package com.agriconnect.Contract.Farming.App.Service;

import com.agriconnect.Contract.Farming.App.DTO.OrderRequest;
import com.agriconnect.Contract.Farming.App.DTO.PageRequest;
import com.agriconnect.Contract.Farming.App.DTO.PageResponse;
import com.agriconnect.Contract.Farming.App.Entity.Order;
import com.agriconnect.Contract.Farming.App.Repository.OrderRepository;
import com.agriconnect.Contract.Farming.App.exception.ResourceNotFoundException;
import com.agriconnect.Contract.Farming.App.kafka.NotificationEventPublisher;
import com.agriconnect.Contract.Farming.App.util.CursorUtil;
import com.agriconnect.notification.avro.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CursorUtil cursorUtil;
    private final NotificationEventPublisher notificationEventPublisher;

    @Value("${notification.topics.contract}")
    private String contractTopic;

    public OrderService(OrderRepository orderRepository,
                        CursorUtil cursorUtil,
                        NotificationEventPublisher notificationEventPublisher) {
        this.orderRepository = orderRepository;
        this.cursorUtil = cursorUtil;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    public Order getOrderById(String orderId) {
        logger.debug("Fetching order by ID: {}", orderId);
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
    }

    public List<Order> getAllOrders() {
        logger.debug("Fetching all orders (deprecated - use pagination instead)");
        return orderRepository.findAll();
    }

    public List<Order> getAllOrdersByUser(String userId) {
        logger.debug("Fetching all orders for user: {}", userId);
        return orderRepository.findOrdersByUser(userId);
    }

    public List<Order> getAllCompletedOrdersByBuyer(String buyerAddress) {
        logger.debug("Fetching completed orders for buyer: {}", buyerAddress);
        return orderRepository.findOrdersByBuyerAddressAndStatus(buyerAddress, "completed");
    }

    public List<Order> getAllCompletedOrdersByFarmer(String farmerAddress) {
        logger.debug("Fetching completed orders for farmer: {}", farmerAddress);
        return orderRepository.findOrdersByFarmerAddressAndStatus(farmerAddress, "completed");
    }

    public Order createOrder(OrderRequest orderRequest) {
        logger.info("Creating order for farmer: {} and buyer: {}", 
                    orderRequest.getFarmerAddress(), orderRequest.getBuyerAddress());
        
        Order order = new Order();
        order.setFarmerAddress(orderRequest.getFarmerAddress());
        order.setBuyerAddress(orderRequest.getBuyerAddress());
        order.setCurrency("INR");
        order.setStatus("created");
        order.setCreatedDate(LocalDate.now());
        order.setCreatedTime(LocalTime.now());
        order.setListingId(orderRequest.getListingId());
        order.setAmount(orderRequest.getAmount());
        order.setQuantity(Long.parseLong(orderRequest.getQuantity()));
        order.setAgreementId(orderRequest.getAgreementId());

        Order savedOrder = orderRepository.save(order);
        logger.info("Order created successfully with ID: {}", savedOrder.getId());

        try {
            Map<String, String> payload = Map.of(
                "orderId",    savedOrder.getId(),
                "amount",     String.valueOf(savedOrder.getAmount()),
                "quantity",   String.valueOf(savedOrder.getQuantity()),
                "createdAt",  Instant.now().toString()
            );
            // Notify farmer
            notificationEventPublisher.publish(contractTopic,
                notificationEventPublisher.buildEvent("CONTRACT_ORDER_CREATED",
                    savedOrder.getFarmerAddress(), "contract.order.created",
                    List.of("EMAIL", "IN_APP"), payload, Priority.HIGH,
                    savedOrder.getId(), null, null));
            // Notify buyer
            notificationEventPublisher.publish(contractTopic,
                notificationEventPublisher.buildEvent("CONTRACT_ORDER_CREATED",
                    savedOrder.getBuyerAddress(), "contract.order.created",
                    List.of("EMAIL", "IN_APP"), payload, Priority.HIGH,
                    savedOrder.getId() + "-buyer", null, null));
        } catch (Exception ex) {
            logger.warn("[NOTIFY] Failed to publish CONTRACT_ORDER_CREATED for order={}: {}", savedOrder.getId(), ex.getMessage());
        }

        return savedOrder;
    }

    // ==================== CURSOR-BASED PAGINATION METHODS ====================

    /**
     * Get paginated orders with cursor-based pagination
     */
    public PageResponse<Order> getOrdersPaginated(PageRequest pageRequest) {
        logger.debug("Fetching paginated orders - cursor: {}, limit: {}", 
                     pageRequest.getCursor(), pageRequest.getLimit());

        List<Order> orders;
        int limit = pageRequest.getLimit();
        
        // Fetch limit + 1 to check if there's a next page
        org.springframework.data.domain.PageRequest springPageRequest =
                org.springframework.data.domain.PageRequest.of(0, limit + 1);

        if (pageRequest.getCursor() == null) {
            // First page
            if ("ASC".equals(pageRequest.getSortDirection())) {
                orders = orderRepository.findOrdersFirstPageAsc(springPageRequest);
            } else {
                orders = orderRepository.findOrdersFirstPageDesc(springPageRequest);
            }
        } else {
            // Subsequent pages
            CursorUtil.CursorData cursorData = cursorUtil.decodeCursor(pageRequest.getCursor());
            
            if ("ASC".equals(pageRequest.getSortDirection())) {
                orders = orderRepository.findOrdersAfterCursorAsc(
                    cursorData.getDate(), cursorData.getTime(), cursorData.getId(), springPageRequest);
            } else {
                orders = orderRepository.findOrdersAfterCursorDesc(
                    cursorData.getDate(), cursorData.getTime(), cursorData.getId(), springPageRequest);
            }
        }

        return buildPageResponse(orders, limit, pageRequest);
    }

    /**
     * Get paginated orders for a specific user
     */
    public PageResponse<Order> getOrdersByUserPaginated(String userId, PageRequest pageRequest) {
        logger.debug("Fetching paginated orders for user: {} - cursor: {}, limit: {}", 
                     userId, pageRequest.getCursor(), pageRequest.getLimit());

        List<Order> orders;
        int limit = pageRequest.getLimit();
        org.springframework.data.domain.PageRequest springPageRequest = 
                org.springframework.data.domain.PageRequest.of(0, limit + 1);

        if (pageRequest.getCursor() == null) {
            // First page
            orders = orderRepository.findOrdersByUserFirstPage(userId, springPageRequest);
        } else {
            // Subsequent pages
            CursorUtil.CursorData cursorData = cursorUtil.decodeCursor(pageRequest.getCursor());
            orders = orderRepository.findOrdersByUserAfterCursor(
                userId, cursorData.getDate(), cursorData.getTime(), cursorData.getId(), springPageRequest);
        }

        return buildPageResponse(orders, limit, pageRequest);
    }

    /**
     * Get paginated orders for a specific buyer with status
     */
    public PageResponse<Order> getOrdersByBuyerPaginated(String buyerAddress, String status, PageRequest pageRequest) {
        logger.debug("Fetching paginated orders for buyer: {} with status: {}", buyerAddress, status);

        List<Order> orders;
        int limit = pageRequest.getLimit();
        org.springframework.data.domain.PageRequest springPageRequest = 
                org.springframework.data.domain.PageRequest.of(0, limit + 1);

        if (pageRequest.getCursor() == null) {
            // First page
            orders = orderRepository.findOrdersByBuyerFirstPage(buyerAddress, status, springPageRequest);
        } else {
            // Subsequent pages
            CursorUtil.CursorData cursorData = cursorUtil.decodeCursor(pageRequest.getCursor());
            orders = orderRepository.findOrdersByBuyerAfterCursor(
                buyerAddress, status, cursorData.getDate(), cursorData.getTime(), 
                cursorData.getId(), springPageRequest);
        }

        return buildPageResponse(orders, limit, pageRequest);
    }

    /**
     * Get paginated orders for a specific farmer with status
     */
    public PageResponse<Order> getOrdersByFarmerPaginated(String farmerAddress, String status, PageRequest pageRequest) {
        logger.debug("Fetching paginated orders for farmer: {} with status: {}", farmerAddress, status);

        List<Order> orders;
        int limit = pageRequest.getLimit();
        org.springframework.data.domain.PageRequest springPageRequest = 
                org.springframework.data.domain.PageRequest.of(0, limit + 1);

        if (pageRequest.getCursor() == null) {
            // First page
            orders = orderRepository.findOrdersByFarmerFirstPage(farmerAddress, status, springPageRequest);
        } else {
            // Subsequent pages
            CursorUtil.CursorData cursorData = cursorUtil.decodeCursor(pageRequest.getCursor());
            orders = orderRepository.findOrdersByFarmerAfterCursor(
                farmerAddress, status, cursorData.getDate(), cursorData.getTime(), 
                cursorData.getId(), springPageRequest);
        }

        return buildPageResponse(orders, limit, pageRequest);
    }

    /**
     * Build page response with metadata
     */
    private PageResponse<Order> buildPageResponse(List<Order> orders, int limit, PageRequest pageRequest) {
        boolean hasNext = orders.size() > limit;
        
        // Remove the extra item we fetched to check for next page
        if (hasNext) {
            orders = orders.subList(0, limit);
        }

        String nextCursor = null;
        String prevCursor = null;

        if (!orders.isEmpty()) {
            // Next cursor is from the last item
            if (hasNext) {
                Order lastOrder = orders.get(orders.size() - 1);
                nextCursor = cursorUtil.encodeCursor(
                    lastOrder.getCreatedDate(), 
                    lastOrder.getCreatedTime(), 
                    lastOrder.getId()
                );
            }

            // Prev cursor is from the first item (for reverse pagination)
            Order firstOrder = orders.get(0);
            prevCursor = cursorUtil.encodeCursor(
                firstOrder.getCreatedDate(), 
                firstOrder.getCreatedTime(), 
                firstOrder.getId()
            );
        }

        PageResponse.PageMetadata metadata = PageResponse.PageMetadata.builder()
                .nextCursor(nextCursor)
                .prevCursor(prevCursor)
                .hasNext(hasNext)
                .hasPrev(pageRequest.getCursor() != null) // If cursor exists, we have previous pages
                .pageSize(limit)
                .returnedCount(orders.size())
                .build();

        return PageResponse.<Order>builder()
                .data(orders)
                .metadata(metadata)
                .build();
    }
}
