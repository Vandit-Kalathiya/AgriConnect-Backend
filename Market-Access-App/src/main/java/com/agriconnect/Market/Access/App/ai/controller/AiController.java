package com.agriconnect.Market.Access.App.ai.controller;

import com.agriconnect.Market.Access.App.ai.config.AiProperties;
import com.agriconnect.Market.Access.App.ai.dto.ChatDtos;
import com.agriconnect.Market.Access.App.ai.dto.CropAdvisoryDtos;
import com.agriconnect.Market.Access.App.ai.dto.ListingDtos;
import com.agriconnect.Market.Access.App.ai.dto.MarketDtos;
import com.agriconnect.Market.Access.App.ai.entity.AiConversationEntity;
import com.agriconnect.Market.Access.App.ai.entity.CropAdvisoryHistoryEntity;
import com.agriconnect.Market.Access.App.ai.entity.KisanMitraHistoryEntity;
import com.agriconnect.Market.Access.App.ai.repository.projection.ChatConversationSummaryProjection;
import com.agriconnect.Market.Access.App.ai.repository.projection.ChatMessageProjection;
import com.agriconnect.Market.Access.App.ai.service.AiHistoryService;
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
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final AiHistoryService aiHistoryService;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    @PostMapping("/chat/respond")
    @Operation(summary = "Generate chat response")
    public ResponseEntity<ChatDtos.ChatRespondResponse> chatRespond(
            @Valid @RequestBody ChatDtos.ChatRespondRequest request,
            @RequestHeader(value = "X-User-Phone", required = false) String userPhone
    ) {
        ensureAuthenticatedForPrivateAi(userPhone);
        AiConversationEntity conversation = conversationStoreService.ensureConversation(
                request.getConversationId(),
                userPhone,
                request.getLanguage()
        );
        ChatDtos.ChatMessage latestUserMessage = extractLatestUserChatMessage(request.getMessages());
        if (latestUserMessage == null) {
            throw new IllegalArgumentException("A non-empty user message is required.");
        }
        conversationStoreService.appendIncomingMessage(conversation, latestUserMessage);
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
        aiHistoryService.saveKisanMitraExchange(
                conversation.getConversationId(),
                userPhone,
                request.getLanguage(),
                latestUserMessage == null ? "" : latestUserMessage.getContent(),
                ai.text(),
                ai.source(),
                ai.safetyDecision()
        );
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
        aiHistoryService.saveCropAdvisoryResponse(
                userPhone,
                request.getLanguage(),
                request.getDistrict(),
                request.getState(),
                safe(request.getSoilType()),
                safe(request.getSeason()),
                toJsonSafe(response),
                ai.source(),
                ai.safetyDecision()
        );
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

    @GetMapping("/chat/history")
    @Operation(summary = "Fetch Kisan Mitra chat history")
    public ResponseEntity<ChatDtos.ChatHistoryResponse> chatHistory(
            @RequestHeader(value = "X-User-Phone", required = false) String userPhone,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        ensureAuthenticatedForPrivateAi(userPhone);
        Page<KisanMitraHistoryEntity> historyPage = aiHistoryService.getKisanMitraHistory(userPhone, page, size);
        List<ChatDtos.ChatHistoryItem> items = historyPage.stream()
                .map(row -> ChatDtos.ChatHistoryItem.builder()
                        .conversationId(row.getConversationId())
                        .userMessage(row.getUserMessage())
                        .assistantResponse(row.getAssistantResponse())
                        .language(row.getLanguage())
                        .source(row.getSource())
                        .safetyDecision(row.getSafetyDecision())
                        .createdAt(row.getCreatedAt())
                        .build())
                .toList();
        return ResponseEntity.ok(ChatDtos.ChatHistoryResponse.builder()
                .schemaVersion(aiProperties.getSchemaVersion())
                .history(items)
                .page(historyPage.getNumber())
                .size(historyPage.getSize())
                .totalElements(historyPage.getTotalElements())
                .totalPages(historyPage.getTotalPages())
                .hasMore(historyPage.hasNext())
                .build());
    }

    @GetMapping("/chat/conversations")
    @Operation(summary = "Fetch chat conversations for authenticated user")
    public ResponseEntity<ChatDtos.ChatConversationListResponse> chatConversations(
            @RequestHeader(value = "X-User-Phone", required = false) String userPhone,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        ensureAuthenticatedForPrivateAi(userPhone);
        Page<ChatConversationSummaryProjection> conversationPage = conversationStoreService.listUserConversations(userPhone, page, size);
        List<ChatDtos.ChatConversationSummaryItem> items = conversationPage.stream()
                .map(conversation -> ChatDtos.ChatConversationSummaryItem.builder()
                        .conversationId(conversation.getConversationId())
                        .title(buildConversationTitle(conversation.getTitle(), conversation.getLastMessagePreview(), conversation.getConversationId()))
                        .lastMessagePreview(truncatePreview(conversation.getLastMessagePreview()))
                        .createdAt(conversation.getCreatedAt())
                        .updatedAt(conversation.getUpdatedAt())
                        .build())
                .toList();
        return ResponseEntity.ok(ChatDtos.ChatConversationListResponse.builder()
                .schemaVersion(aiProperties.getSchemaVersion())
                .conversations(items)
                .page(conversationPage.getNumber())
                .size(conversationPage.getSize())
                .totalElements(conversationPage.getTotalElements())
                .totalPages(conversationPage.getTotalPages())
                .hasMore(conversationPage.hasNext())
                .build());
    }

    @PatchMapping("/chat/conversations/{conversationId}/title")
    @Operation(summary = "Rename a chat conversation")
    public ResponseEntity<ChatDtos.RenameConversationResponse> renameConversation(
            @RequestHeader(value = "X-User-Phone", required = false) String userPhone,
            @PathVariable("conversationId") String conversationId,
            @Valid @RequestBody ChatDtos.RenameConversationRequest request
    ) {
        ensureAuthenticatedForPrivateAi(userPhone);
        AiConversationEntity conversation = conversationStoreService.renameConversation(
                userPhone,
                conversationId,
                request.getTitle()
        );
        if (conversation == null) {
            throw new IllegalArgumentException("Conversation not found for authenticated user.");
        }
        return ResponseEntity.ok(ChatDtos.RenameConversationResponse.builder()
                .schemaVersion(aiProperties.getSchemaVersion())
                .conversationId(conversation.getConversationId())
                .title(safe(conversation.getTitle()))
                .updatedAt(conversation.getUpdatedAt())
                .build());
    }

    @GetMapping("/chat/conversations/{conversationId}/messages")
    @Operation(summary = "Fetch messages for a specific chat conversation")
    public ResponseEntity<ChatDtos.ChatConversationMessagesResponse> conversationMessages(
            @RequestHeader(value = "X-User-Phone", required = false) String userPhone,
            @PathVariable("conversationId") String conversationId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size
    ) {
        ensureAuthenticatedForPrivateAi(userPhone);
        Page<ChatMessageProjection> messagePage =
                conversationStoreService.listConversationMessages(userPhone, conversationId, page, size);
        List<ChatDtos.ChatConversationMessageItem> items = messagePage.stream()
                .map(row -> ChatDtos.ChatConversationMessageItem.builder()
                        .sequenceNo(row.getSequenceNo())
                        .role(row.getRole())
                        .content(row.getContent())
                        .source(row.getSource())
                        .safetyDecision(row.getSafetyDecision())
                        .createdAt(row.getCreatedAt())
                        .build())
                .toList();
        return ResponseEntity.ok(ChatDtos.ChatConversationMessagesResponse.builder()
                .schemaVersion(aiProperties.getSchemaVersion())
                .conversationId(conversationId)
                .messages(items)
                .page(messagePage.getNumber())
                .size(messagePage.getSize())
                .totalElements(messagePage.getTotalElements())
                .totalPages(messagePage.getTotalPages())
                .hasMore(messagePage.hasNext())
                .build());
    }

    @GetMapping("/crop/history")
    @Operation(summary = "Fetch crop advisory history")
    public ResponseEntity<CropAdvisoryDtos.CropAdvisoryHistoryResponse> cropHistory(
            @RequestHeader(value = "X-User-Phone", required = false) String userPhone,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        ensureAuthenticatedForPrivateAi(userPhone);
        Page<CropAdvisoryHistoryEntity> historyPage = aiHistoryService.getCropAdvisoryHistory(userPhone, page, size);
        List<CropAdvisoryDtos.CropAdvisoryHistoryItem> items = historyPage.stream()
                .map(row -> CropAdvisoryDtos.CropAdvisoryHistoryItem.builder()
                        .district(row.getDistrict())
                        .state(row.getState())
                        .soilType(row.getSoilType())
                        .season(row.getSeason())
                        .language(row.getLanguage())
                        .responsePayload(row.getResponsePayload())
                        .source(row.getSource())
                        .safetyDecision(row.getSafetyDecision())
                        .createdAt(row.getCreatedAt())
                        .build())
                .toList();
        return ResponseEntity.ok(CropAdvisoryDtos.CropAdvisoryHistoryResponse.builder()
                .schemaVersion(aiProperties.getSchemaVersion())
                .history(items)
                .page(historyPage.getNumber())
                .size(historyPage.getSize())
                .totalElements(historyPage.getTotalElements())
                .totalPages(historyPage.getTotalPages())
                .hasMore(historyPage.hasNext())
                .build());
    }

    @DeleteMapping("/chat/history")
    @Operation(summary = "Delete Kisan Mitra chat history for authenticated user")
    public ResponseEntity<Map<String, Object>> deleteChatHistory(
            @RequestHeader(value = "X-User-Phone", required = false) String userPhone,
            @RequestParam(value = "conversationId", required = false) String conversationId
    ) {
        ensureAuthenticatedForPrivateAi(userPhone);
        long deleted = (conversationId == null || conversationId.isBlank())
                ? aiHistoryService.deleteKisanMitraHistory(userPhone)
                : aiHistoryService.deleteKisanMitraConversationHistory(userPhone, conversationId);
        String scope = (conversationId == null || conversationId.isBlank()) ? "all-chat-history" : "conversation";
        return ResponseEntity.ok(Map.of(
                "schemaVersion", aiProperties.getSchemaVersion(),
                "scope", scope,
                "conversationId", conversationId == null ? "" : conversationId,
                "deletedCount", deleted
        ));
    }

    @DeleteMapping("/chat/conversations/{conversationId}")
    @Operation(summary = "Delete a single chat conversation for authenticated user")
    public ResponseEntity<Map<String, Object>> deleteConversation(
            @RequestHeader(value = "X-User-Phone", required = false) String userPhone,
            @PathVariable("conversationId") String conversationId
    ) {
        ensureAuthenticatedForPrivateAi(userPhone);
        long deletedConversation = conversationStoreService.deleteConversation(userPhone, conversationId);
        long deletedKisanHistory = aiHistoryService.deleteConversationHistory(userPhone, conversationId);
        return ResponseEntity.ok(Map.of(
                "schemaVersion", aiProperties.getSchemaVersion(),
                "conversationId", conversationId,
                "deletedConversation", deletedConversation,
                "deletedKisanMitraHistory", deletedKisanHistory
        ));
    }

    @DeleteMapping("/history/all")
    @Operation(summary = "Delete all AI histories for authenticated user")
    public ResponseEntity<Map<String, Object>> deleteAllHistories(
            @RequestHeader(value = "X-User-Phone", required = false) String userPhone
    ) {
        ensureAuthenticatedForPrivateAi(userPhone);
        AiHistoryService.DeleteSummary summary = aiHistoryService.deleteAllHistories(userPhone);
        return ResponseEntity.ok(Map.of(
                "schemaVersion", aiProperties.getSchemaVersion(),
                "deletedKisanMitra", summary.deletedKisanMitra(),
                "deletedCropAdvisory", summary.deletedCropAdvisory(),
                "totalDeleted", summary.totalDeleted()
        ));
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

    private ChatDtos.ChatMessage extractLatestUserChatMessage(List<ChatDtos.ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatDtos.ChatMessage message = messages.get(i);
            if (message != null && "user".equalsIgnoreCase(message.getRole()) && message.getContent() != null && !message.getContent().isBlank()) {
                return message;
            }
        }
        return null;
    }

    private String truncatePreview(String text) {
        String value = safe(text);
        return value.length() > 120 ? value.substring(0, 120) : value;
    }

    private String buildConversationTitle(String explicitTitle, String preview, String conversationId) {
        String explicit = safe(explicitTitle).trim();
        if (!explicit.isBlank()) {
            return explicit.length() > 140 ? explicit.substring(0, 140) : explicit;
        }
        String clean = safe(preview).trim();
        if (!clean.isBlank()) {
            return clean.length() > 40 ? clean.substring(0, 40) : clean;
        }
        return "Chat " + safe(conversationId);
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
