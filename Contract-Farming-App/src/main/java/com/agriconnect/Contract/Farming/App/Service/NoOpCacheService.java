package com.agriconnect.Contract.Farming.App.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@ConditionalOnProperty(name = "redis.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpCacheService extends CacheService {

    private static final Logger logger = LoggerFactory.getLogger(NoOpCacheService.class);

    public NoOpCacheService() {
        super(null);
        logger.info("NoOpCacheService initialized - all cache operations will be no-ops");
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        return Optional.empty();
    }

    @Override
    public void save(String key, Object value, Duration ttl) {
    }

    @Override
    public void evict(String key) {
    }

    @Override
    public void evictPattern(String pattern) {
    }

    @Override
    public boolean exists(String key) {
        return false;
    }

    @Override
    public void setExpire(String key, Duration ttl) {
    }

    @Override
    public Long increment(String key) {
        return 0L;
    }

    @Override
    public Long decrement(String key) {
        return 0L;
    }
}
