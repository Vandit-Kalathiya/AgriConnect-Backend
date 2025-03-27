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
