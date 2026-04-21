package com.agriconnect.Contract.Farming.App.Service;

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
        logger.info("CacheService initialized with Redis/Valkey support");
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        if (!redisEnabled) {
            return Optional.empty();
        }

        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null && type.isInstance(value)) {
                logger.debug("Cache hit for key: {}", key);
                return Optional.of(type.cast(value));
            }
            logger.debug("Cache miss for key: {}", key);
            return Optional.empty();
        } catch (RedisConnectionFailureException e) {
            logger.warn("Redis connection failed while getting key: {}. Degrading gracefully.", key);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error getting cache key: {}", key, e);
            return Optional.empty();
        }
    }

    public void save(String key, Object value, Duration ttl) {
        if (!redisEnabled) {
            return;
        }

        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            logger.debug("Cached key: {} with TTL: {}", key, ttl);
        } catch (RedisConnectionFailureException e) {
            logger.warn("Redis connection failed while saving key: {}. Degrading gracefully.", key);
        } catch (Exception e) {
            logger.error("Error saving cache key: {}", key, e);
        }
    }

    public void evict(String key) {
        if (!redisEnabled) {
            return;
        }

        try {
            redisTemplate.delete(key);
            logger.debug("Evicted cache key: {}", key);
        } catch (RedisConnectionFailureException e) {
            logger.warn("Redis connection failed while evicting key: {}. Degrading gracefully.", key);
        } catch (Exception e) {
            logger.error("Error evicting cache key: {}", key, e);
        }
    }

    public void evictPattern(String pattern) {
        if (!redisEnabled) {
            return;
        }

        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.info("Evicted {} keys matching pattern: {}", keys.size(), pattern);
            }
        } catch (RedisConnectionFailureException e) {
            logger.warn("Redis connection failed while evicting pattern: {}. Degrading gracefully.", pattern);
        } catch (Exception e) {
            logger.error("Error evicting cache pattern: {}", pattern, e);
        }
    }

    public boolean exists(String key) {
        if (!redisEnabled) {
            return false;
        }

        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (RedisConnectionFailureException e) {
            logger.warn("Redis connection failed while checking key existence: {}. Degrading gracefully.", key);
            return false;
        } catch (Exception e) {
            logger.error("Error checking cache key existence: {}", key, e);
            return false;
        }
    }

    public void setExpire(String key, Duration ttl) {
        if (!redisEnabled) {
            return;
        }

        try {
            redisTemplate.expire(key, ttl);
            logger.debug("Set expiration for key: {} to {}", key, ttl);
        } catch (RedisConnectionFailureException e) {
            logger.warn("Redis connection failed while setting expiration for key: {}. Degrading gracefully.", key);
        } catch (Exception e) {
            logger.error("Error setting expiration for cache key: {}", key, e);
        }
    }

    public Long increment(String key) {
        if (!redisEnabled) {
            return 0L;
        }

        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (RedisConnectionFailureException e) {
            logger.warn("Redis connection failed while incrementing key: {}. Degrading gracefully.", key);
            return 0L;
        } catch (Exception e) {
            logger.error("Error incrementing cache key: {}", key, e);
            return 0L;
        }
    }

    public Long decrement(String key) {
        if (!redisEnabled) {
            return 0L;
        }

        try {
            return redisTemplate.opsForValue().decrement(key);
        } catch (RedisConnectionFailureException e) {
            logger.warn("Redis connection failed while decrementing key: {}. Degrading gracefully.", key);
            return 0L;
        } catch (Exception e) {
            logger.error("Error decrementing cache key: {}", key, e);
            return 0L;
        }
    }
}
