package com.agriconnect.api.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(RedisHealthIndicator.class);

    private final RedisConnectionFactory connectionFactory;

    public RedisHealthIndicator(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Health health() {
        try {
            String pong = connectionFactory.getConnection().ping();
            
            if ("PONG".equalsIgnoreCase(pong)) {
                logger.debug("Redis/Valkey health check: UP");
                return Health.up()
                        .withDetail("service", "Redis/Valkey")
                        .withDetail("status", "Connected")
                        .withDetail("response", pong)
                        .build();
            } else {
                logger.warn("Redis/Valkey health check: Unexpected response - {}", pong);
                return Health.down()
                        .withDetail("service", "Redis/Valkey")
                        .withDetail("status", "Unexpected response")
                        .withDetail("response", pong)
                        .build();
            }
        } catch (Exception e) {
            logger.error("Redis/Valkey health check: DOWN - {}", e.getMessage());
            return Health.down()
                    .withDetail("service", "Redis/Valkey")
                    .withDetail("status", "Connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
