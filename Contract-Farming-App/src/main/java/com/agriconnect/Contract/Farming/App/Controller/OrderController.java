package com.agriconnect.Contract.Farming.App.Controller;

import com.agriconnect.Contract.Farming.App.DTO.OrderRequest;
import com.agriconnect.Contract.Farming.App.DTO.PageRequest;
import com.agriconnect.Contract.Farming.App.DTO.PageResponse;
import com.agriconnect.Contract.Farming.App.Entity.Order;
import com.agriconnect.Contract.Farming.App.Service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@Validated
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/create")
    public ResponseEntity<Order> createOrder(@Valid @RequestBody OrderRequest orderRequest) {
        logger.info("Creating order for listing: {}", orderRequest.getListingId());
        Order createdOrder = orderService.createOrder(orderRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable String id) {
        logger.debug("Fetching order by ID: {}", id);
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    /**
     * Get all orders with cursor-based pagination (NEW - Recommended)
     * Query params:
     *   - cursor: Pagination cursor (optional, omit for first page)
     *   - limit: Number of records per page (default: 20, max: 100)
     *   - sortDirection: ASC or DESC (default: DESC - newest first)
     * 
     * Example: GET /orders/paginated?limit=20&sortDirection=DESC
     *          GET /orders/paginated?cursor=encodedCursor&limit=20
     */
    @GetMapping("/paginated")
    public ResponseEntity<PageResponse<Order>> getAllOrdersPaginated(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection) {
        
        logger.info("Fetching paginated orders - cursor: {}, limit: {}, sort: {}", cursor, limit, sortDirection);
        
        PageRequest pageRequest = PageRequest.builder()
                .cursor(cursor)
                .limit(limit)
                .sortDirection(sortDirection)
                .build();
        
        PageResponse<Order> response = orderService.getOrdersPaginated(pageRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all orders (deprecated - use /paginated instead for better performance)
     */
    @GetMapping("/all")
    @Deprecated
    public ResponseEntity<List<Order>> getAllOrders() {
        logger.warn("Using deprecated endpoint /all - consider using /paginated instead");
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * Get user's orders with cursor-based pagination
     * Query params same as /paginated
     */
    @GetMapping("/u/{userId}/paginated")
    public ResponseEntity<PageResponse<Order>> getAllOrdersByUserPaginated(
            @PathVariable String userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection) {
        
        logger.info("Fetching paginated orders for user: {}", userId);
        
        PageRequest pageRequest = PageRequest.builder()
                .cursor(cursor)
                .limit(limit)
                .sortDirection(sortDirection)
                .build();
        
        PageResponse<Order> response = orderService.getOrdersByUserPaginated(userId, pageRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's orders (deprecated - use /u/{userId}/paginated instead)
     */
    @GetMapping("/u/{userId}")
    @Deprecated
    public ResponseEntity<List<Order>> getAllOrdersByFarmer(@PathVariable String userId) {
        logger.warn("Using deprecated endpoint /u/{userId} - consider using /u/{userId}/paginated instead");
        List<Order> order = orderService.getAllOrdersByUser(userId);
        return ResponseEntity.ok(order);
    }

    /**
     * Get buyer's completed orders with pagination
     */
    @GetMapping("/buyer/{buyerAddress}/paginated")
    public ResponseEntity<PageResponse<Order>> getOrdersByBuyerPaginated(
            @PathVariable String buyerAddress,
            @RequestParam(required = false, defaultValue = "completed") String status,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        
        logger.info("Fetching paginated orders for buyer: {} with status: {}", buyerAddress, status);
        
        PageRequest pageRequest = PageRequest.builder()
                .cursor(cursor)
                .limit(limit)
                .sortDirection("DESC")
                .build();
        
        PageResponse<Order> response = orderService.getOrdersByBuyerPaginated(buyerAddress, status, pageRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Get buyer's orders (deprecated - use /buyer/{buyerAddress}/paginated instead)
     */
    @GetMapping("/buyer/{buyerAddress}")
    @Deprecated
    public ResponseEntity<List<Order>> getPaymentsByBuyerAddress(@PathVariable String buyerAddress) {
        logger.warn("Using deprecated endpoint - consider using /buyer/{buyerAddress}/paginated");
        return ResponseEntity.ok(orderService.getAllCompletedOrdersByBuyer(buyerAddress));
    }

    /**
     * Get farmer's completed orders with pagination
     */
    @GetMapping("/farmer/{farmerAddress}/paginated")
    public ResponseEntity<PageResponse<Order>> getOrdersByFarmerPaginated(
            @PathVariable String farmerAddress,
            @RequestParam(required = false, defaultValue = "completed") String status,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        
        logger.info("Fetching paginated orders for farmer: {} with status: {}", farmerAddress, status);
        
        PageRequest pageRequest = PageRequest.builder()
                .cursor(cursor)
                .limit(limit)
                .sortDirection("DESC")
                .build();
        
        PageResponse<Order> response = orderService.getOrdersByFarmerPaginated(farmerAddress, status, pageRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Get farmer's orders (deprecated - use /farmer/{farmerAddress}/paginated instead)
     */
    @GetMapping("/farmer/{farmerAddress}")
    @Deprecated
    public ResponseEntity<List<Order>> getPaymentsByFarmerAddress(@PathVariable String farmerAddress) {
        logger.warn("Using deprecated endpoint - consider using /farmer/{farmerAddress}/paginated");
        return ResponseEntity.ok(orderService.getAllCompletedOrdersByFarmer(farmerAddress));
    }
}
