package com.agriconnect.Market.Access.App.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ListingListResponse {
    private String id;
    private String productName;
    private String productDescription;
    private String productType;
    private Double finalPrice;
    private Double aiGeneratedPrice;
    private LocalDate harvestedDate;
    private LocalDate availabilityDate;
    private String qualityGrade;
    private String storageCondition;
    private Long quantity;
    private String unitOfQuantity;
    private String location;
    private String certifications;
    private Long shelfLifetime;
    private String contactOfFarmer;
    private Double rating;
    private String status;
    private LocalDate createdDate;
    private LocalDate lastUpdatedDate;
    private LocalTime createdTime;
    private Integer imageCount;
    private String thumbnailFileName;
}
