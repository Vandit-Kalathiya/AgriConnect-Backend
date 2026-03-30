package com.agriconnect.Market.Access.App.ai.service;

import com.agriconnect.Market.Access.App.ai.config.AiProperties;
import com.agriconnect.Market.Access.App.ai.dto.AiEnums;
import com.agriconnect.Market.Access.App.ai.service.provider.LlmProviderAdapter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.StringJoiner;

@Service
public class AiOrchestrationService {

    private final LlmProviderAdapter providerAdapter;
    private final SafetyPolicyService safetyPolicyService;
    private final PromptTemplateRegistry promptTemplateRegistry;
    private final AiCacheService cacheService;
    private final AiProperties aiProperties;
    private final Counter fallbackCounter;
    private final Counter cacheHitCounter;

    public AiOrchestrationService(
            LlmProviderAdapter providerAdapter,
            SafetyPolicyService safetyPolicyService,
            PromptTemplateRegistry promptTemplateRegistry,
            AiCacheService cacheService,
            AiProperties aiProperties,
            MeterRegistry meterRegistry
    ) {
        this.providerAdapter = providerAdapter;
        this.safetyPolicyService = safetyPolicyService;
        this.promptTemplateRegistry = promptTemplateRegistry;
        this.cacheService = cacheService;
        this.aiProperties = aiProperties;
        this.fallbackCounter = meterRegistry.counter("ai_fallback_total");
        this.cacheHitCounter = meterRegistry.counter("ai_cache_hit_total");
    }

    public AiResult generate(String templateKey, Map<String, Object> variables, boolean cacheable) {
        String prompt = buildPrompt(templateKey, variables);
        AiEnums.SafetyDecision decision = safetyPolicyService.evaluate(prompt);
        if (decision != AiEnums.SafetyDecision.ALLOW) {
            fallbackCounter.increment();
            return new AiResult(
                    "I can help only with agriculture-related topics. Please ask about farming, crops, market rates, soil, weather, or storage.",
                    decision,
                    AiEnums.ResponseSource.FALLBACK
            );
        }

        String cacheKey = templateKey + "::" + safetyPolicyService.redactPii(variables.toString());
        if (cacheable) {
            var cached = cacheService.get(cacheKey);
            if (cached.isPresent()) {
                cacheHitCounter.increment();
                return new AiResult(cached.get(), AiEnums.SafetyDecision.ALLOW, AiEnums.ResponseSource.CACHE);
            }
        }

        int attempts = Math.max(1, aiProperties.getRetries() + 1);
        Exception lastException = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                String output = providerAdapter.generate(prompt);
                if (cacheable) {
                    cacheService.put(cacheKey, output, aiProperties.getCacheTtlSeconds());
                }
                return new AiResult(output, AiEnums.SafetyDecision.ALLOW, AiEnums.ResponseSource.LLM);
            } catch (Exception ex) {
                lastException = ex;
            }
        }

        fallbackCounter.increment();
        String fallbackText = "Service is temporarily unavailable. Please retry in a moment.";
        if (lastException != null) {
            fallbackText = fallbackText + " (" + lastException.getClass().getSimpleName() + ")";
        }
        return new AiResult(fallbackText, AiEnums.SafetyDecision.ALLOW, AiEnums.ResponseSource.FALLBACK);
    }

    private String buildPrompt(String templateKey, Map<String, Object> variables) {
        String template = promptTemplateRegistry.get(templateKey);
        String rendered = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("Policy: Reply only in agriculture context. Refuse out-of-scope prompts.");
        joiner.add("Template: " + rendered);
        return joiner.toString();
    }

    public record AiResult(String text, AiEnums.SafetyDecision safetyDecision, AiEnums.ResponseSource source) {
    }
}
