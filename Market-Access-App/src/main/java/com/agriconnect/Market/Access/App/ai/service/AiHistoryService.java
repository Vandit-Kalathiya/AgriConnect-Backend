package com.agriconnect.Market.Access.App.ai.service;

import com.agriconnect.Market.Access.App.ai.config.AiProperties;
import com.agriconnect.Market.Access.App.ai.dto.AiEnums;
import com.agriconnect.Market.Access.App.ai.entity.CropAdvisoryHistoryEntity;
import com.agriconnect.Market.Access.App.ai.entity.KisanMitraHistoryEntity;
import com.agriconnect.Market.Access.App.ai.repository.CropAdvisoryHistoryRepository;
import com.agriconnect.Market.Access.App.ai.repository.KisanMitraHistoryRepository;
import org.springframework.data.domain.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiHistoryService {

    private final KisanMitraHistoryRepository kisanMitraHistoryRepository;
    private final CropAdvisoryHistoryRepository cropAdvisoryHistoryRepository;
    private final SafetyPolicyService safetyPolicyService;
    private final AiProperties aiProperties;

    @Transactional
    public void saveKisanMitraExchange(
            String conversationId,
            String userPhone,
            String language,
            String userMessage,
            String assistantResponse,
            AiEnums.ResponseSource source,
            AiEnums.SafetyDecision safetyDecision
    ) {
        if (!aiProperties.getPersistence().isEnabled() || userPhone == null || userPhone.isBlank()) {
            return;
        }
        kisanMitraHistoryRepository.save(KisanMitraHistoryEntity.builder()
                .conversationId(conversationId)
                .userPhone(userPhone)
                .language(language)
                .userMessage(truncate(safetyPolicyService.redactPii(userMessage)))
                .assistantResponse(truncate(safetyPolicyService.redactPii(assistantResponse)))
                .source(source == null ? null : source.name())
                .safetyDecision(safetyDecision == null ? null : safetyDecision.name())
                .build());
    }

    @Transactional
    public void saveCropAdvisoryResponse(
            String userPhone,
            String language,
            String district,
            String state,
            String soilType,
            String season,
            String responsePayload,
            AiEnums.ResponseSource source,
            AiEnums.SafetyDecision safetyDecision
    ) {
        if (!aiProperties.getPersistence().isEnabled() || userPhone == null || userPhone.isBlank()) {
            return;
        }
        cropAdvisoryHistoryRepository.save(CropAdvisoryHistoryEntity.builder()
                .userPhone(userPhone)
                .language(language)
                .district(truncate(district))
                .state(truncate(state))
                .soilType(truncate(soilType))
                .season(truncate(season))
                .responsePayload(truncate(safetyPolicyService.redactPii(responsePayload)))
                .source(source == null ? null : source.name())
                .safetyDecision(safetyDecision == null ? null : safetyDecision.name())
                .build());
    }

    @Transactional(readOnly = true)
    public Page<KisanMitraHistoryEntity> getKisanMitraHistory(String userPhone, int page, int size) {
        if (!aiProperties.getPersistence().isEnabled() || userPhone == null || userPhone.isBlank()) {
            return Page.empty();
        }
        int sanitizedSize = sanitizeSize(size);
        int sanitizedPage = sanitizePage(page);
        return kisanMitraHistoryRepository.findByUserPhoneOrderByCreatedAtDesc(
                userPhone,
                PageRequest.of(sanitizedPage, sanitizedSize)
        );
    }

    @Transactional(readOnly = true)
    public Page<CropAdvisoryHistoryEntity> getCropAdvisoryHistory(String userPhone, int page, int size) {
        if (!aiProperties.getPersistence().isEnabled() || userPhone == null || userPhone.isBlank()) {
            return Page.empty();
        }
        int sanitizedSize = sanitizeSize(size);
        int sanitizedPage = sanitizePage(page);
        return cropAdvisoryHistoryRepository.findByUserPhoneOrderByCreatedAtDesc(
                userPhone,
                PageRequest.of(sanitizedPage, sanitizedSize)
        );
    }

    @Transactional
    public long deleteKisanMitraHistory(String userPhone) {
        if (!aiProperties.getPersistence().isEnabled() || userPhone == null || userPhone.isBlank()) {
            return 0;
        }
        return kisanMitraHistoryRepository.deleteByUserPhone(userPhone);
    }

    @Transactional
    public long deleteKisanMitraConversationHistory(String userPhone, String conversationId) {
        if (!aiProperties.getPersistence().isEnabled()
                || userPhone == null || userPhone.isBlank()
                || conversationId == null || conversationId.isBlank()) {
            return 0;
        }
        return kisanMitraHistoryRepository.deleteByUserPhoneAndConversationId(userPhone, conversationId);
    }

    @Transactional
    public long deleteConversationHistory(String userPhone, String conversationId) {
        return deleteKisanMitraConversationHistory(userPhone, conversationId);
    }

    @Transactional
    public DeleteSummary deleteAllHistories(String userPhone) {
        if (!aiProperties.getPersistence().isEnabled() || userPhone == null || userPhone.isBlank()) {
            return new DeleteSummary(0, 0);
        }
        long deletedChat = kisanMitraHistoryRepository.deleteByUserPhone(userPhone);
        long deletedCrop = cropAdvisoryHistoryRepository.deleteByUserPhone(userPhone);
        return new DeleteSummary(deletedChat, deletedCrop);
    }

    private int sanitizeSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private int sanitizePage(int page) {
        return Math.max(page, 0);
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        int maxLength = aiProperties.getPersistence().getMaxContentLength();
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    public record DeleteSummary(long deletedKisanMitra, long deletedCropAdvisory) {
        public long totalDeleted() {
            return deletedKisanMitra + deletedCropAdvisory;
        }
    }
}
