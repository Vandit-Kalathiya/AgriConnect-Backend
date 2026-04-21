package com.agriconnect.Contract.Farming.App.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "agreement", indexes = {
        @Index(name = "idx_agreement_tx_hash", columnList = "transactionHash", unique = true),
        @Index(name = "idx_agreement_pdf_hash", columnList = "pdfHash", unique = true),
        @Index(name = "idx_agreement_order_id", columnList = "orderId", unique = true),
        @Index(name = "idx_agreement_farmer_addr", columnList = "farmerAddress"),
        @Index(name = "idx_agreement_buyer_addr", columnList = "buyerAddress"),
        @Index(name = "idx_agreement_created", columnList = "createDate,createTime,id")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Agreement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String fileName;
    private String fileType;
    private Long size;
    private String downloadUrl;
    private String orderId;
    private String farmerAddress;
    private String buyerAddress;
    private String transactionHash = "";
    private String pdfHash;

    @Lob
    private byte[] data;

    public Agreement(String farmerAddress, String buyerAddress, String orderId, Long size, String fileName,
            String fileType, byte[] data, LocalDate createDate, LocalTime createTime, String transactionHash,
            String pdfHash) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.data = data;
        this.createDate = createDate;
        this.createTime = createTime;
        this.size = size;
        this.transactionHash = transactionHash;
        this.pdfHash = pdfHash;
        this.orderId = orderId;
        this.farmerAddress = farmerAddress;
        this.buyerAddress = buyerAddress;
        // this.downloadUrl = downloadUrl;
    }

    private LocalDate createDate;

    private LocalTime createTime;
}
