package com.agriconnect.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Service
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true", matchIfMissing = false)
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${redis.enabled:false}")
    private boolean redisEnabled;

    @Autowired
    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        logger.info("CacheService initialized for Notification-Service");
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        if (!redisEnabled) return Optional.empty();
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null && type.isInstance(value)) {
                logger.debug("Cache hit for key: {}", key);
                return Optional.of(type.cast(value));
            }
            return Optional.empty();
        } catch (RedisConnectionFailureException e) {
            logger.warn("Redis unavailable on get key: {}, degrading gracefully", key);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Cache get error for key: {}", key, e);
            return Optional.empty();
        }
    }

    public void save(String key, Object value, Duration ttl) {
        if (!redisEnabled) return;
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            logger.debug("Cached key: {} TTL: {}", key, ttl);
        } catch (RedisConnectionFailureException e) {
            logger.warn("Redis unavailable on save key: {}, degrading gracefully", key);
        } catch (Exception e) {
            logger.error("Cache save error for key: {}", key, e);
        }
    }

    public void evict(String key) {
        if (!redisEnabled) return;
        try {
            redisTemplate.delete(key);
            logger.debug("Evicted cache key: {}", key);
        } catch (Exception e) {
            logger.warn("Cache evict error for key: {}", key, e);
        }
    }

    public void evictPattern(String pattern) {
        if (!redisEnabled) return;
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.info("Evicted {} keys matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            logger.warn("Cache evictPattern error for pattern: {}", pattern, e);
        }
    }

    public boolean exists(String key) {
        if (!redisEnabled) return false;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            return false;
        }
    }

    public void setExpire(String key, Duration ttl) {
        if (!redisEnabled) return;
        try {
            redisTemplate.expire(key, ttl);
        } catch (Exception e) {
            logger.warn("Cache setExpire error for key: {}", key, e);
        }
    }

    public Long increment(String key) {
        if (!redisEnabled) return 0L;
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            return 0L;
        }
    }
}
