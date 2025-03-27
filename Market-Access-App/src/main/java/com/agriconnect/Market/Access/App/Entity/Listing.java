package com.agriconnect.Market.Access.App.Entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "listings")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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
    private Double rating = 0.0;
    private String status = String.valueOf(ListingStatus.ACTIVE);
    private LocalDate createdDate;
    private LocalDate lastUpdatedDate;
    private LocalTime createdTime;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<Image> images = new ArrayList<>();

    public void addImage(Image image) {
        this.images.add(image);
        image.setListing(this);
    }

    public void removeImage(Image image) {
        this.images.remove(image);
        image.setListing(null);
    }
}
