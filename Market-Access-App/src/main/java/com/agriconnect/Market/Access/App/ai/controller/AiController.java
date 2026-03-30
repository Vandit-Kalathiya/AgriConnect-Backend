package com.agriconnect.Market.Access.App.ai.controller;

import com.agriconnect.Market.Access.App.ai.config.AiProperties;
import com.agriconnect.Market.Access.App.ai.dto.AiEnums;
import com.agriconnect.Market.Access.App.ai.dto.ChatDtos;
import com.agriconnect.Market.Access.App.ai.dto.CropAdvisoryDtos;
import com.agriconnect.Market.Access.App.ai.dto.ListingDtos;
import com.agriconnect.Market.Access.App.ai.dto.MarketDtos;
import com.agriconnect.Market.Access.App.ai.entity.AiConversationEntity;
import com.agriconnect.Market.Access.App.ai.service.AiOrchestrationService;
import com.agriconnect.Market.Access.App.ai.service.ConversationStoreService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
@Tag(name = "AI Orchestration", description = "Centralized AI endpoints for chatbot, advisory, market and listing assistance")
public class AiController {

    private final AiOrchestrationService orchestrationService;
    private final ConversationStoreService conversationStoreService;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    @PostMapping("/chat/respond")
    @Operation(summary = "Generate chat response")
    public ResponseEntity<ChatDtos.ChatRespondResponse> chatRespond(
            @Valid @RequestBody ChatDtos.ChatRespondRequest request,
            @RequestHeader(value = "X-User-Phone", required = false) String userPhone
    ) {
        AiConversationEntity conversation = conversationStoreService.ensureConversation(
                request.getConversationId(),
                userPhone,
                request.getLanguage()
        );
        conversationStoreService.appendIncomingMessages(conversation, request.getMessages());
        List<ChatDtos.ChatMessage> mergedMessages = conversationStoreService.loadRecentChatContext(
                conversation.getConversationId(),
                aiProperties.getPersistence().getContextWindow()
        );
        if (mergedMessages.isEmpty()) {
            mergedMessages = request.getMessages();
        }
        String mergedText = mergedMessages.stream().map(ChatDtos.ChatMessage::getContent).reduce("", (a, b) -> a + "\n" + b);
        var ai = orchestrationService.generate("chat", Map.of("language", request.getLanguage(), "messages", mergedText), false);
        conversationStoreService.appendAssistantResponse(conversation, ai.text(), ai.source(), ai.safetyDecision());
        ChatDtos.ChatRespondResponse response = ChatDtos.ChatRespondResponse.builder()
                .schemaVersion(aiProperties.getSchemaVersion())
                .text(ai.text())
                .conversationId(conversation.getConversationId())
                .safetyDecision(ai.safetyDecision())
                .source(ai.source())
                .build();
        conversationStoreService.appendEndpointInteraction(
                "CHAT",
                userPhone,
                request.getLanguage(),
                toJsonSafe(request),
                toJsonSafe(response),
                ai.source(),
                ai.safetyDecision()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/crop/recommendations")
    @Operation(summary = "Generate crop recommendations")
    public ResponseEntity<CropAdvisoryDtos.CropRecommendationResponse> cropRecommendations(
            @Valid @RequestBody CropAdvisoryDtos.CropRecommendationRequest request,
            @RequestHeader(value = "X-User-Phone", required = false) String userPhone
    ) {
        ensureAuthenticatedForPrivateAi(userPhone);
        var ai = orchestrationService.generate("crop-recommendations", Map.of(
                "district", request.getDistrict(),
                "state", request.getState(),
                "soilType", safe(request.getSoilType()),
                "season", safe(request.getSeason()),
                "language", request.getLanguage()
        ), true);

        CropAdvisoryDtos.CropRecommendationItem item = CropAdvisoryDtos.CropRecommendationItem.builder()
                .cropName(extractSeed(ai.text(), "cropName", "Wheat"))
                .reason(extractSeed(ai.text(), "reason", "Suitable based on climate and demand"))
                .confidence("medium")
                .build();

        CropAdvisoryDtos.CropRecommendationResponse response = CropAdvisoryDtos.CropRecommendationResponse.builder()
                .schemaVersion(aiProperties.getSchemaVersion())
                .crops(List.of(item))
                .safetyDecision(ai.safetyDecision())
                .source(ai.source())
                .build();
        conversationStoreService.appendEndpointInteraction(
                "CROP_RECOMMENDATIONS",
                userPhone,
                request.getLanguage(),
                toJsonSafe(request),
                toJsonSafe(response),
                ai.source(),
                ai.safetyDecision()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/market/crop-analysis")
    @Operation(summary = "Generate market crop analysis")
    public ResponseEntity<MarketDtos.CropAnalysisResponse> marketCropAnalysis(
            @Valid @RequestBody MarketDtos.CropAnalysisRequest request,
            @RequestHeader(value = "X-User-Phone", required = false) String userPhone
    ) {
        ensureAuthenticatedForPrivateAi(userPhone);
        var ai = orchestrationService.generate("market-analysis", Map.of(
                "cropName", request.getCropName(),
                "region", safe(request.getRegion()),
                "timeframe", safe(request.getTimeframe())
        ), true);
        MarketDtos.CropAnalysisResponse response = MarketDtos.CropAnalysisResponse.builder()
                .schemaVersion(aiProperties.getSchemaVersion())
                .predictions(extractSeed(ai.text(), "predictions", "Moderate demand with stable short-term movement"))
                .recommendations(Arrays.asList("Monitor local mandi arrivals", "Stagger sales across weeks"))
                .marketInsights(extractSeed(ai.text(), "marketInsights", "Regional procurement and weather are key drivers"))
                .priceAnalysis(extractSeed(ai.text(), "priceAnalysis", "Price likely stable with slight upside"))
                .safetyDecision(ai.safetyDecision())
                .source(ai.source())
                .build();
        conversationStoreService.appendEndpointInteraction(
                "MARKET_CROP_ANALYSIS",
                userPhone,
                null,
                toJsonSafe(request),
                toJsonSafe(response),
                ai.source(),
                ai.safetyDecision()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/market/recommendations")
    @Operation(summary = "Generate market recommendations")
    public ResponseEntity<MarketDtos.MarketRecommendationsResponse> marketRecommendations(
            @Valid @RequestBody MarketDtos.MarketRecommendationsRequest request,
            @RequestHeader(value = "X-User-Phone", required = false) String userPhone
    ) {
        ensureAuthenticatedForPrivateAi(userPhone);
        var ai = orchestrationService.generate("market-recommendations", Map.of(
                "cropType", request.getCropType(),
                "season", safe(request.getSeason()),
                "region", safe(request.getRegion())
        ), true);
        MarketDtos.MarketRecommendationsResponse response = MarketDtos.MarketRecommendationsResponse.builder()
                .schemaVersion(aiProperties.getSchemaVersion())
                .recommendations(Arrays.asList(
                        "Compare two nearby markets before dispatch",
                        "Track daily arrivals and avoid peak glut windows",
                        extractSeed(ai.text(), "recommendation", "Use graded packaging for better realization")
                ))
                .safetyDecision(ai.safetyDecision())
                .source(ai.source())
                .build();
        conversationStoreService.appendEndpointInteraction(
                "MARKET_RECOMMENDATIONS",
                userPhone,
                null,
                toJsonSafe(request),
                toJsonSafe(response),
                ai.source(),
                ai.safetyDecision()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/listing/shelf-life")
    @Operation(summary = "Predict listing shelf life")
    public ResponseEntity<ListingDtos.ShelfLifeResponse> listingShelfLife(
            @Valid @RequestBody ListingDtos.ShelfLifeRequest request,
            @RequestHeader(value = "X-User-Phone", required = false) String userPhone
    ) {
        ensureAuthenticatedForPrivateAi(userPhone);
        var ai = orchestrationService.generate("listing-shelf-life", Map.of(
                "productName", request.getProductName(),
                "cropType", request.getCropType(),
                "storageConditions", safe(request.getStorageConditions())
        ), true);
        ListingDtos.ShelfLifeResponse response = ListingDtos.ShelfLifeResponse.builder()
                .schemaVersion(aiProperties.getSchemaVersion())
                .shelfLifeDays(7)
                .confidence("medium")
                .notes(extractSeed(ai.text(), "notes", "Keep produce in cool, dry and ventilated storage"))
                .safetyDecision(ai.safetyDecision())
                .source(ai.source())
                .build();
        conversationStoreService.appendEndpointInteraction(
                "LISTING_SHELF_LIFE",
                userPhone,
                null,
                toJsonSafe(request),
                toJsonSafe(response),
                ai.source(),
                ai.safetyDecision()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/listing/price-suggestion")
    @Operation(summary = "Generate listing price suggestion")
    public ResponseEntity<ListingDtos.PriceSuggestionResponse> listingPriceSuggestion(
            @Valid @RequestBody ListingDtos.PriceSuggestionRequest request,
            @RequestHeader(value = "X-User-Phone", required = false) String userPhone
    ) {
        ensureAuthenticatedForPrivateAi(userPhone);
        var ai = orchestrationService.generate("listing-price", Map.of(
                "productName", request.getProductName(),
                "qualityGrade", request.getQualityGrade(),
                "quantityKg", request.getQuantityKg(),
                "location", request.getLocation()
        ), true);
        ListingDtos.PriceSuggestionResponse response = ListingDtos.PriceSuggestionResponse.builder()
                .schemaVersion(aiProperties.getSchemaVersion())
                .pricePerKg(new BigDecimal("32.50"))
                .recommendations(List.of(
                        "List early in the day for higher visibility",
                        extractSeed(ai.text(), "priceTip", "Highlight quality grade in title")
                ))
                .nearbyFarmers(List.of())
                .safetyDecision(ai.safetyDecision())
                .source(ai.source())
                .build();
        conversationStoreService.appendEndpointInteraction(
                "LISTING_PRICE_SUGGESTION",
                userPhone,
                null,
                toJsonSafe(request),
                toJsonSafe(response),
                ai.source(),
                ai.safetyDecision()
        );
        return ResponseEntity.ok(response);
    }

    private void ensureAuthenticatedForPrivateAi(String userPhone) {
        if (userPhone == null || userPhone.isBlank()) {
            throw new IllegalArgumentException("Authenticated context is required for this AI endpoint.");
        }
    }

    private String safe(String input) {
        return input == null ? "" : input;
    }

    private String extractSeed(String text, String keyword, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        if (text.toLowerCase().contains(keyword.toLowerCase())) {
            return text.length() > 180 ? text.substring(0, 180) : text;
        }
        return fallback;
    }

    private String toJsonSafe(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}
