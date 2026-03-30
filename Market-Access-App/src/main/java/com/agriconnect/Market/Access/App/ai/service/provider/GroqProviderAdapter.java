package com.agriconnect.Market.Access.App.ai.service.provider;

import com.agriconnect.Market.Access.App.ai.config.AiProperties;
import com.groq.sdk.client.GroqClient;
import com.groq.sdk.models.GroqResponse;
import com.groq.sdk.models.chat.ChatCompletion;
import com.groq.sdk.models.chat.ChatCompletionRequest;
import com.groq.sdk.models.chat.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GroqProviderAdapter implements LlmProviderAdapter {

    private final AiProperties aiProperties;

    @Override
    public String generate(String prompt) {
        if (!aiProperties.isEnabled() || aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            return "AI service is operating in fallback mode right now. Please try again shortly.";
        }

        GroqClient client = GroqClient.builder()
                .apiKey(aiProperties.getApiKey())
                .baseUrl(aiProperties.getBaseUrl())
                .timeout(Duration.ofMillis(aiProperties.getTimeoutMs()))
                .maxRetries(aiProperties.getRetries())
                .build();

        ChatMessage message = new ChatMessage("user", prompt);
        ChatCompletionRequest request = new ChatCompletionRequest(aiProperties.getModel(), List.of(message));
        request.setTemperature(0.2);
        request.setMaxTokens(800);

        GroqResponse<ChatCompletion> response = client.chat().createCompletion(request);
        if (!response.isSuccessful() || response.getData() == null
                || response.getData().getChoices() == null
                || response.getData().getChoices().isEmpty()
                || response.getData().getChoices().get(0).getMessage() == null
                || response.getData().getChoices().get(0).getMessage().getContent() == null) {
            throw new IllegalStateException("Invalid response from Groq provider");
        }
        return response.getData().getChoices().get(0).getMessage().getContent();
    }
}
