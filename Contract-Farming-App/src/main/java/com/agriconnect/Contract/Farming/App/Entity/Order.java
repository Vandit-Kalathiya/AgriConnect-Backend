package com.agriconnect.Contract.Farming.App.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String pdfHash; // From your blockchain agreement
    private String listingId;
    private String farmerAddress;
    private String buyerAddress;
    private Long amount; // In smallest unit (paise for INR)
    private String currency;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
    private String status; // created, paid_pending_delivery, delivered, verified, completed, rejected, refunded
    private String trackingNumber; // Optional: for tracking delivery
    private String returnTrackingNumber; // For return to farmer
    private String razorpayRefundId; // Refund ID if processed

    private LocalDate createdDate;
    private LocalTime createdTime;
}
