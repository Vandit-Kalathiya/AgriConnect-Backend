package com.agriconnect.Market.Access.App.ai.service;

import com.agriconnect.Market.Access.App.ai.config.AiProperties;
import com.agriconnect.Market.Access.App.ai.repository.AiMessageRepository;
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

    @Scheduled(cron = "${ai.persistence.cleanup-cron:0 0 3 * * *}")
    public void scheduledCleanup() {
        if (!aiProperties.getPersistence().isEnabled() || !aiProperties.getPersistence().isCleanupEnabled()) {
            return;
        }
        long totalDeleted = 0;
        int maxBatches = Math.max(1, aiProperties.getPersistence().getCleanupMaxBatches());
        int batchSize = Math.max(100, aiProperties.getPersistence().getCleanupBatchSize());
        for (int i = 0; i < maxBatches; i++) {
            long deleted = cleanupOldMessages(batchSize);
            totalDeleted += deleted;
            if (deleted < batchSize) {
                break;
            }
        }
        if (totalDeleted > 0) {
            log.info("AI persistence cleanup removed {} old rows", totalDeleted);
        }
    }
}
