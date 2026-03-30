package com.agriconnect.Market.Access.App.ai.service;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PromptTemplateRegistry {

    private final Map<String, String> templates = Map.of(
            "chat", "You are Kisan Mitra, agriculture-only assistant. Respond in {{language}}. Reject non-agri topics politely.",
            "crop-recommendations", "Suggest crops for district={{district}}, state={{state}}, soil={{soilType}}, season={{season}}.",
            "market-analysis", "Analyze market for crop={{cropName}}, region={{region}}, timeframe={{timeframe}}.",
            "market-recommendations", "Give concise actionable recommendations for crop={{cropType}}, season={{season}}, region={{region}}.",
            "listing-shelf-life", "Estimate shelf life for product={{productName}}, crop={{cropType}}, storage={{storageConditions}}.",
            "listing-price", "Suggest fair price per kg for product={{productName}}, grade={{qualityGrade}}, quantityKg={{quantityKg}}, location={{location}}."
    );

    public String get(String templateKey) {
        return templates.getOrDefault(templateKey, "");
    }
}
