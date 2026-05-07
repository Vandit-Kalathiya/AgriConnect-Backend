package com.agriconnect.Generate.Agreement.App.Config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "cache.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpCacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(NoOpCacheConfig.class);

    public NoOpCacheConfig() {
        logger.warn("To enable caching, set cache.enabled=true in application properties");
    }

    @Bean
    public CacheManager cacheManager() {
        return new NoOpCacheManager();
    }
}
