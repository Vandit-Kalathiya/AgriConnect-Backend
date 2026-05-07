package com.agriconnect.Market.Access.App.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ListingResponse {
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
    private List<ImageInfo> images;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ImageInfo {
        private String id;
        private String fileName;
        private String fileType;
        private Long size;
        private String downloadUrl;
        private byte[] data;
        private LocalDate createDate;
        private LocalTime createTime;
    }
}
