package com.agriconnect.Contract.Farming.App.Repository;

import com.agriconnect.Contract.Farming.App.Entity.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    Order findByRazorpayOrderId(String razorpayOrderId);
    Order findByPdfHash(String pdfHash);

    // Find all orders by both farmerAddress and buyerAddress
    List<Order> findByFarmerAddressAndBuyerAddress(String farmerAddress, String buyerAddress);

    @Query("SELECT o FROM Order o WHERE o.farmerAddress = :userId OR o.buyerAddress = :userId")
    List<Order> findOrdersByUser(@Param("userId") String userId);

    List<Order> findOrdersByBuyerAddressAndStatus(String buyerAddress, String completed);

    List<Order> findOrdersByFarmerAddressAndStatus(String farmerAddress, String completed);

    // ==================== CURSOR-BASED PAGINATION ====================
    
    // Get orders with cursor pagination (descending - newest first)
    @Query("SELECT o FROM Order o WHERE " +
           "(o.createdDate < :cursorDate) OR " +
           "(o.createdDate = :cursorDate AND o.createdTime < :cursorTime) OR " +
           "(o.createdDate = :cursorDate AND o.createdTime = :cursorTime AND o.id < :cursorId) " +
           "ORDER BY o.createdDate DESC, o.createdTime DESC, o.id DESC")
    List<Order> findOrdersAfterCursorDesc(
            @Param("cursorDate") LocalDate cursorDate,
            @Param("cursorTime") LocalTime cursorTime,
            @Param("cursorId") String cursorId,
            Pageable pageable);

    // Get orders with cursor pagination (ascending - oldest first)
    @Query("SELECT o FROM Order o WHERE " +
           "(o.createdDate > :cursorDate) OR " +
           "(o.createdDate = :cursorDate AND o.createdTime > :cursorTime) OR " +
           "(o.createdDate = :cursorDate AND o.createdTime = :cursorTime AND o.id > :cursorId) " +
           "ORDER BY o.createdDate ASC, o.createdTime ASC, o.id ASC")
    List<Order> findOrdersAfterCursorAsc(
            @Param("cursorDate") LocalDate cursorDate,
            @Param("cursorTime") LocalTime cursorTime,
            @Param("cursorId") String cursorId,
            Pageable pageable);

    // First page descending
    @Query("SELECT o FROM Order o ORDER BY o.createdDate DESC, o.createdTime DESC, o.id DESC")
    List<Order> findOrdersFirstPageDesc(Pageable pageable);

    // First page ascending
    @Query("SELECT o FROM Order o ORDER BY o.createdDate ASC, o.createdTime ASC, o.id ASC")
    List<Order> findOrdersFirstPageAsc(Pageable pageable);

    // By User ID with cursor pagination
    @Query("SELECT o FROM Order o WHERE (o.farmerAddress = :userId OR o.buyerAddress = :userId) AND " +
           "((o.createdDate < :cursorDate) OR " +
           "(o.createdDate = :cursorDate AND o.createdTime < :cursorTime) OR " +
           "(o.createdDate = :cursorDate AND o.createdTime = :cursorTime AND o.id < :cursorId)) " +
           "ORDER BY o.createdDate DESC, o.createdTime DESC, o.id DESC")
    List<Order> findOrdersByUserAfterCursor(
            @Param("userId") String userId,
            @Param("cursorDate") LocalDate cursorDate,
            @Param("cursorTime") LocalTime cursorTime,
            @Param("cursorId") String cursorId,
            Pageable pageable);

    @Query("SELECT o FROM Order o WHERE (o.farmerAddress = :userId OR o.buyerAddress = :userId) " +
           "ORDER BY o.createdDate DESC, o.createdTime DESC, o.id DESC")
    List<Order> findOrdersByUserFirstPage(@Param("userId") String userId, Pageable pageable);

    // By Buyer with cursor pagination
    @Query("SELECT o FROM Order o WHERE o.buyerAddress = :buyerAddress AND o.status = :status AND " +
           "((o.createdDate < :cursorDate) OR " +
           "(o.createdDate = :cursorDate AND o.createdTime < :cursorTime) OR " +
           "(o.createdDate = :cursorDate AND o.createdTime = :cursorTime AND o.id < :cursorId)) " +
           "ORDER BY o.createdDate DESC, o.createdTime DESC, o.id DESC")
    List<Order> findOrdersByBuyerAfterCursor(
            @Param("buyerAddress") String buyerAddress,
            @Param("status") String status,
            @Param("cursorDate") LocalDate cursorDate,
            @Param("cursorTime") LocalTime cursorTime,
            @Param("cursorId") String cursorId,
            Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.buyerAddress = :buyerAddress AND o.status = :status " +
           "ORDER BY o.createdDate DESC, o.createdTime DESC, o.id DESC")
    List<Order> findOrdersByBuyerFirstPage(
            @Param("buyerAddress") String buyerAddress,
            @Param("status") String status,
            Pageable pageable);

    // By Farmer with cursor pagination
    @Query("SELECT o FROM Order o WHERE o.farmerAddress = :farmerAddress AND o.status = :status AND " +
           "((o.createdDate < :cursorDate) OR " +
           "(o.createdDate = :cursorDate AND o.createdTime < :cursorTime) OR " +
           "(o.createdDate = :cursorDate AND o.createdTime = :cursorTime AND o.id < :cursorId)) " +
           "ORDER BY o.createdDate DESC, o.createdTime DESC, o.id DESC")
    List<Order> findOrdersByFarmerAfterCursor(
            @Param("farmerAddress") String farmerAddress,
            @Param("status") String status,
            @Param("cursorDate") LocalDate cursorDate,
            @Param("cursorTime") LocalTime cursorTime,
            @Param("cursorId") String cursorId,
            Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.farmerAddress = :farmerAddress AND o.status = :status " +
           "ORDER BY o.createdDate DESC, o.createdTime DESC, o.id DESC")
    List<Order> findOrdersByFarmerFirstPage(
            @Param("farmerAddress") String farmerAddress,
            @Param("status") String status,
            Pageable pageable);
}
