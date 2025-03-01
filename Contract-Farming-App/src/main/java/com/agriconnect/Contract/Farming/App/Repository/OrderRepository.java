package com.agriconnect.Contract.Farming.App.Repository;

import com.agriconnect.Contract.Farming.App.Entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    Order findByRazorpayOrderId(String razorpayOrderId);
    Order findByPdfHash(String pdfHash);

    // Find all orders by farmerAddress
//    List<Order> findByFarmerAddress(String farmerAddress);
//
//    // Find all orders by buyerAddress
//    List<Order> findByBuyerAddress(String buyerAddress);

    // Find all orders by both farmerAddress and buyerAddress
    List<Order> findByFarmerAddressAndBuyerAddress(String farmerAddress, String buyerAddress);

//    Order findByBuyerAddress(String buyerAddress);
    @Query("SELECT o FROM Order o WHERE o.farmerAddress = :userId OR o.buyerAddress = :userId")
    List<Order> findOrdersByUser(@Param("userId") String userId);
}
