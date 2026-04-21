package com.agriconnect.Contract.Farming.App.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_farmer_addr", columnList = "farmerAddress"),
        @Index(name = "idx_order_buyer_addr", columnList = "buyerAddress"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_razorpay_id", columnList = "razorpayOrderId"),
        @Index(name = "idx_order_pdf_hash", columnList = "pdfHash"),
        @Index(name = "idx_order_created", columnList = "createdDate,createdTime,id"),
        @Index(name = "idx_order_farmer_status", columnList = "farmerAddress,status"),
        @Index(name = "idx_order_buyer_status", columnList = "buyerAddress,status")
})
@Getter
@Setter
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String pdfHash;
    private String listingId;
    private String farmerAddress;
    private String buyerAddress;
    private Long quantity;
    private Long amount;
    private String currency;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
    private String status;
    private String trackingNumber;
    private String returnTrackingNumber;
    private String razorpayRefundId;
    private String agreementId;

    private LocalDate createdDate;
    private LocalTime createdTime;
}
