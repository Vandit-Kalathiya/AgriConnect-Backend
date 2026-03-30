package com.agriconnect.Market.Access.App.ai.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ChatDtos {

    private ChatDtos() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        @NotBlank
        @JsonAlias({"type"})
        private String role;
        @NotBlank
        @JsonAlias({"text"})
        private String content;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRespondRequest {
        @Valid
        @NotEmpty
        private List<ChatMessage> messages;
        @NotBlank
        private String language;
        private String conversationId;
        private Map<String, Object> userContext;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRespondResponse {
        private String schemaVersion;
        private String text;
        private String conversationId;
        private AiEnums.SafetyDecision safetyDecision;
        private AiEnums.ResponseSource source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatHistoryItem {
        private String conversationId;
        private String userMessage;
        private String assistantResponse;
        private String language;
        private String source;
        private String safetyDecision;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatHistoryResponse {
        private String schemaVersion;
        private List<ChatHistoryItem> history;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean hasMore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatConversationSummaryItem {
        private String conversationId;
        private String title;
        private String lastMessagePreview;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatConversationListResponse {
        private String schemaVersion;
        private List<ChatConversationSummaryItem> conversations;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean hasMore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatConversationMessageItem {
        private Long sequenceNo;
        private String role;
        private String content;
        private String source;
        private String safetyDecision;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatConversationMessagesResponse {
        private String schemaVersion;
        private String conversationId;
        private List<ChatConversationMessageItem> messages;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean hasMore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RenameConversationRequest {
        @NotBlank
        @Size(max = 140)
        private String title;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RenameConversationResponse {
        private String schemaVersion;
        private String conversationId;
        private String title;
        private LocalDateTime updatedAt;
    }
}
