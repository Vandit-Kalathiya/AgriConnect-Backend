package com.agriconnect.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;

import java.util.List;

@Configuration
public class RetryConfig {

    /**
     * Shared retry template for all notification dispatchers.
     * Strategy: 3 attempts with exponential backoff starting at 1s,
     * multiplier 2, capped at 8s — handles transient SMTP / Twilio / FCM errors.
     */
    @Bean
    public RetryTemplate notificationRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(1000, 2.0, 8000)
                .withListeners(List.of(retryLoggingListener()))
                .build();
    }

    private RetryListener retryLoggingListener() {
        return new RetryListener() {
            @Override
            public <T, E extends Throwable> void onError(RetryContext ctx, RetryCallback<T, E> callback, Throwable t) {
                // Logged by each dispatcher — no-op here to avoid double logging
            }
        };
    }
}
