package com.agriconnect.Market.Access.App.ai.dto;

public final class AiEnums {

    private AiEnums() {
    }

    public enum SafetyDecision {
        ALLOW,
        BLOCKED_OUT_OF_DOMAIN,
        BLOCKED_INJECTION,
        BLOCKED_POLICY
    }

    public enum ResponseSource {
        LLM,
        CACHE,
        FALLBACK
    }
}
