package com.agriconnect.Market.Access.App.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public final class CropAdvisoryDtos {

    private CropAdvisoryDtos() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CropRecommendationRequest {
        @NotBlank
        private String district;
        @NotBlank
        private String state;
        private String soilType;
        private String season;
        @NotBlank
        private String language;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CropRecommendationItem {
        private String cropName;
        private String reason;
        private String confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CropRecommendationResponse {
        private String schemaVersion;
        private List<CropRecommendationItem> crops;
        private AiEnums.SafetyDecision safetyDecision;
        private AiEnums.ResponseSource source;
    }
}
