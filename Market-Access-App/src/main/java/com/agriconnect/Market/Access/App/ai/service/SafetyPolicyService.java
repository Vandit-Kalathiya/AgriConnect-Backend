package com.agriconnect.Market.Access.App.ai.service;

import com.agriconnect.Market.Access.App.ai.dto.AiEnums;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
public class SafetyPolicyService {

    private static final Set<String> AGRI_KEYWORDS = Set.of(
            "crop", "farming", "soil", "irrigation", "fertilizer", "pesticide",
            "mandi", "harvest", "seed", "weather", "agriculture", "livestock",
            "market", "price", "farmer", "shelf life", "storage", "yield"
    );

    private static final Set<String> INJECTION_PATTERNS = Set.of(
            "ignore previous instructions",
            "reveal system prompt",
            "developer message",
            "jailbreak",
            "bypass safety"
    );

    public AiEnums.SafetyDecision evaluate(String text) {
        String normalized = normalize(text);
        if (containsAny(normalized, INJECTION_PATTERNS)) {
            return AiEnums.SafetyDecision.BLOCKED_INJECTION;
        }
        if (!containsAny(normalized, AGRI_KEYWORDS)) {
            return AiEnums.SafetyDecision.BLOCKED_OUT_OF_DOMAIN;
        }
        return AiEnums.SafetyDecision.ALLOW;
    }

    public String redactPii(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replaceAll("\\b\\d{10}\\b", "[REDACTED_PHONE]")
                .replaceAll("[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+", "[REDACTED_EMAIL]");
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String value, Set<String> patterns) {
        for (String pattern : patterns) {
            if (value.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}
