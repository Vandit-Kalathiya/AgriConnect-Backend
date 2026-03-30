package com.agriconnect.Market.Access.App.ai.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public final class ListingDtos {

    private ListingDtos() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShelfLifeRequest {
        @NotBlank
        private String productName;
        @NotBlank
        private String cropType;
        private String storageConditions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShelfLifeResponse {
        private String schemaVersion;
        private Integer shelfLifeDays;
        private String confidence;
        private String notes;
        private AiEnums.SafetyDecision safetyDecision;
        private AiEnums.ResponseSource source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceSuggestionRequest {
        @NotBlank
        private String productName;
        @NotBlank
        private String qualityGrade;
        @Min(1)
        private Integer quantityKg;
        @NotBlank
        private String location;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceSuggestionResponse {
        private String schemaVersion;
        private BigDecimal pricePerKg;
        private List<String> recommendations;
        private List<String> nearbyFarmers;
        private AiEnums.SafetyDecision safetyDecision;
        private AiEnums.ResponseSource source;
    }
}
