package com.agriconnect.Market.Access.App.ai.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AiStartupSettingsLogger {

    private static final Logger log = LoggerFactory.getLogger(AiStartupSettingsLogger.class);

    private final AiProperties aiProperties;

    @Bean
    public ApplicationRunner aiSettingsLogRunner() {
        return args -> {
            boolean apiKeyConfigured = aiProperties.getApiKey() != null && !aiProperties.getApiKey().isBlank();
            log.info(
                    "AI settings: enabled={}, provider={}, model={}, timeoutMs={}, retries={}, cacheTtlSeconds={}, apiKeyConfigured={}",
                    aiProperties.isEnabled(),
                    aiProperties.getProvider(),
                    aiProperties.getModel(),
                    aiProperties.getTimeoutMs(),
                    aiProperties.getRetries(),
                    aiProperties.getCacheTtlSeconds(),
                    apiKeyConfigured
            );
            log.info(
                    "AI persistence: enabled={}, contextWindow={}, retentionDays={}, maxContentLength={}, cleanupEnabled={}, cleanupBatchSize={}, cleanupMaxBatches={}, cleanupCron={}",
                    aiProperties.getPersistence().isEnabled(),
                    aiProperties.getPersistence().getContextWindow(),
                    aiProperties.getPersistence().getRetentionDays(),
                    aiProperties.getPersistence().getMaxContentLength(),
                    aiProperties.getPersistence().isCleanupEnabled(),
                    aiProperties.getPersistence().getCleanupBatchSize(),
                    aiProperties.getPersistence().getCleanupMaxBatches(),
                    aiProperties.getPersistence().getCleanupCron()
            );
        };
    }
}
