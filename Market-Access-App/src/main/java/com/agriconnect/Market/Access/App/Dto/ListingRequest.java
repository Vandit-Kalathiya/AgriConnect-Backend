package com.agriconnect.Market.Access.App.Dto;

import com.agriconnect.Market.Access.App.Entity.Image;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class ListingRequest {

    private String productName;
    private String productDescription;
    private String productType;
    private String finalPrice;
//    private String aiGeneratedPrice;
    private String harvestedDate;
    private String availabilityDate;
    private String qualityGrade;
    private String storageCondition;
    private String quantity;
    private String unitOfQuantity;
    private String location;
    private String certifications;
    private String shelfLifetime;
    private String contactOfFarmer;
    private List<MultipartFile> images = new ArrayList<>();
}
