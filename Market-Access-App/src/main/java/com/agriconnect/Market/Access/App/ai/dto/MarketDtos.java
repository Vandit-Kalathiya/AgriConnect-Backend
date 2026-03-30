package com.agriconnect.Market.Access.App.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public final class MarketDtos {

    private MarketDtos() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CropAnalysisRequest {
        @NotBlank
        private String cropName;
        private String region;
        private String timeframe;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CropAnalysisResponse {
        private String schemaVersion;
        private String predictions;
        private List<String> recommendations;
        private String marketInsights;
        private String priceAnalysis;
        private AiEnums.SafetyDecision safetyDecision;
        private AiEnums.ResponseSource source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketRecommendationsRequest {
        @NotBlank
        private String cropType;
        private String season;
        private String region;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketRecommendationsResponse {
        private String schemaVersion;
        private List<String> recommendations;
        private AiEnums.SafetyDecision safetyDecision;
        private AiEnums.ResponseSource source;
    }
}
