package com.agriconnect.Market.Access.App.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
        private String role;
        @NotBlank
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
}
