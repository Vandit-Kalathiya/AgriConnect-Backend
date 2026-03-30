package com.agriconnect.Market.Access.App.ai.service;

import com.agriconnect.Market.Access.App.ai.config.AiProperties;
import com.agriconnect.Market.Access.App.ai.repository.AiConversationRepository;
import com.agriconnect.Market.Access.App.ai.repository.AiMessageRepository;
import com.agriconnect.Market.Access.App.ai.repository.CropAdvisoryHistoryRepository;
import com.agriconnect.Market.Access.App.ai.repository.KisanMitraHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AiPersistenceMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(AiPersistenceMaintenanceService.class);

    private final AiMessageRepository aiMessageRepository;
    private final KisanMitraHistoryRepository kisanMitraHistoryRepository;
    private final CropAdvisoryHistoryRepository cropAdvisoryHistoryRepository;
    private final AiConversationRepository aiConversationRepository;
    private final AiProperties aiProperties;

    @Transactional
    public long cleanupOldMessages(int batchSize) {
        int retentionDays = aiProperties.getPersistence().getRetentionDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        var ids = aiMessageRepository.findIdsForCleanup(cutoff, PageRequest.of(0, batchSize));
        if (ids.isEmpty()) {
            return 0;
        }
        return aiMessageRepository.deleteByIdIn(ids);
    }

    @Transactional
    public int cleanupOldKisanHistoryRows(int batchSize) {
        int retentionDays = aiProperties.getPersistence().getRetentionDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        return kisanMitraHistoryRepository.deleteOldRows(cutoff, batchSize);
    }

    @Transactional
    public int cleanupOldCropHistoryRows(int batchSize) {
        int retentionDays = aiProperties.getPersistence().getRetentionDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        return cropAdvisoryHistoryRepository.deleteOldRows(cutoff, batchSize);
    }

    @Transactional
    public int cleanupOldOrphanConversations(int batchSize) {
        int retentionDays = aiProperties.getPersistence().getRetentionDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        return aiConversationRepository.deleteOldOrphanConversations(cutoff, batchSize);
    }

    @Scheduled(cron = "${ai.persistence.cleanup-cron:0 0 3 * * *}")
    public void scheduledCleanup() {
        if (!aiProperties.getPersistence().isEnabled() || !aiProperties.getPersistence().isCleanupEnabled()) {
            return;
        }
        long totalDeleted = 0;
        long deletedMessages = 0;
        long deletedKisanHistory = 0;
        long deletedCropHistory = 0;
        long deletedOrphanConversations = 0;
        int maxBatches = Math.max(1, aiProperties.getPersistence().getCleanupMaxBatches());
        int batchSize = Math.max(100, aiProperties.getPersistence().getCleanupBatchSize());
        for (int i = 0; i < maxBatches; i++) {
            long deleted = cleanupOldMessages(batchSize);
            deletedMessages += deleted;
            totalDeleted += deleted;
            if (deleted < batchSize) {
                break;
            }
        }
        for (int i = 0; i < maxBatches; i++) {
            int deleted = cleanupOldKisanHistoryRows(batchSize);
            deletedKisanHistory += deleted;
            totalDeleted += deleted;
            if (deleted < batchSize) {
                break;
            }
        }
        for (int i = 0; i < maxBatches; i++) {
            int deleted = cleanupOldCropHistoryRows(batchSize);
            deletedCropHistory += deleted;
            totalDeleted += deleted;
            if (deleted < batchSize) {
                break;
            }
        }
        for (int i = 0; i < maxBatches; i++) {
            int deleted = cleanupOldOrphanConversations(batchSize);
            deletedOrphanConversations += deleted;
            totalDeleted += deleted;
            if (deleted < batchSize) {
                break;
            }
        }
        if (totalDeleted > 0) {
            log.info(
                    "AI persistence cleanup removed {} rows (messages={}, kisan_history={}, crop_history={}, orphan_conversations={})",
                    totalDeleted,
                    deletedMessages,
                    deletedKisanHistory,
                    deletedCropHistory,
                    deletedOrphanConversations
            );
        }
    }
}
