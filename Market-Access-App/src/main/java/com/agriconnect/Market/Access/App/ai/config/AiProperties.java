package com.agriconnect.Market.Access.App.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private boolean enabled = true;
    private String schemaVersion = "1.0.0";
    private String provider = "groq";
    private String model = "llama-3.3-70b-versatile";
    private Integer timeoutMs = 12000;
    private Integer retries = 2;
    private Integer chatHistoryLimit = 12;
    private String apiKey = "";
    private String baseUrl = "https://api.groq.com";
    private Integer cacheTtlSeconds = 120;
    private Integer chatPublicPerMinute = 20;
    private Persistence persistence = new Persistence();

    @Data
    public static class Persistence {
        private boolean enabled = true;
        private Integer contextWindow = 12;
        private Integer retentionDays = 30;
        private Integer maxContentLength = 3900;
        private boolean cleanupEnabled = true;
        private Integer cleanupBatchSize = 1000;
        private Integer cleanupMaxBatches = 5;
        private String cleanupCron = "0 0 3 * * *";
    }
}
