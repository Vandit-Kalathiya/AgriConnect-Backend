package com.agriconnect.api.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "redis.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpCacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(NoOpCacheConfig.class);

    @Bean
    public CacheManager cacheManager() {
        logger.warn("Redis/Valkey caching is DISABLED. Using NoOpCacheManager. All @Cacheable annotations will be no-ops.");
        logger.warn("To enable caching, set redis.enabled=true in application properties");
        return new NoOpCacheManager();
    }
}
