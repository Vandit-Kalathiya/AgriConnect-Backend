package com.agriconnect.Generate.Agreement.App.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "cold_storages")
@Data
public class ColdStorage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "place_id", unique = true)
    private String placeId;

    private String name;

    @Column(name = "business_status")
    private String businessStatus;

    private double lat;
    private double lon;

    @Column(name = "viewport_northeast_lat")
    private Double viewportNortheastLat;

    @Column(name = "viewport_northeast_lng")
    private Double viewportNortheastLng;

    @Column(name = "viewport_southwest_lat")
    private Double viewportSouthwestLat;

    @Column(name = "viewport_southwest_lng")
    private Double viewportSouthwestLng;

    private String icon;

    @Column(name = "icon_background_color")
    private String iconBackgroundColor;

    @Column(name = "icon_mask_base_uri")
    private String iconMaskBaseUri;

    @Column(name = "open_now")
    private Boolean openNow;

    @Column(columnDefinition = "TEXT")
    private String photos;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "user_ratings_total")
    private Integer userRatingsTotal;

    @Column(name = "compound_code")
    private String compoundCode;

    @Column(name = "global_code")
    private String globalCode;

    private String vicinity;

    @Column(columnDefinition = "TEXT")
    private String types;

    private String phoneNumber;
//    private String location;
    private String temperature;
    private String specialty;
    private String capacity;
    private Double distance;

}