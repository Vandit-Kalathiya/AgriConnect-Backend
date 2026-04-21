package com.agriconnect.api.gateway.Service.Cache;

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
import java.util.concurrent.TimeUnit;

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
            logger.debug("Cache is disabled, returning empty for key: {}", key);
            return Optional.empty();
        }

        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                logger.debug("Cache miss for key: {}", key);
                return Optional.empty();
            }
            
            logger.debug("Cache hit for key: {}", key);
            return Optional.of(type.cast(value));
        } catch (RedisConnectionFailureException e) {
            logger.warn("Redis connection failed while getting key: {}. Degrading gracefully.", key, e);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Unexpected error while getting cache key: {}", key, e);
            return Optional.empty();
        }
    }

    public void save(String key, Object value) {
        save(key, value, Duration.ofHours(1));
    }

    public void save(String key, Object value, Duration ttl) {
        if (!redisEnabled) {
            logger.debug("Cache is disabled, skipping save for key: {}", key);
            return;
        }

        if (value == null) {
            logger.warn("Attempted to cache null value for key: {}. Skipping.", key);
            return;
        }

        try {
            redisTemplate.opsForValue().set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
            logger.debug("Cached key: {} with TTL: {}", key, ttl);
        } catch (RedisConnectionFailureException e) {
            logger.warn("Redis connection failed while saving key: {}. Degrading gracefully.", key, e);
        } catch (Exception e) {
            logger.error("Unexpected error while saving cache key: {}", key, e);
        }
    }

    public void evict(String key) {
        if (!redisEnabled) {
            logger.debug("Cache is disabled, skipping eviction for key: {}", key);
            return;
        }

        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                logger.debug("Evicted cache key: {}", key);
            } else {
                logger.debug("Cache key not found for eviction: {}", key);
            }
        } catch (RedisConnectionFailureException e) {
            logger.warn("Redis connection failed while evicting key: {}. Degrading gracefully.", key, e);
        } catch (Exception e) {
            logger.error("Unexpected error while evicting cache key: {}", key, e);
        }
    }

    public void evictPattern(String pattern) {
        if (!redisEnabled) {
            logger.debug("Cache is disabled, skipping pattern eviction for: {}", pattern);
            return;
        }

        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                Long deletedCount = redisTemplate.delete(keys);
                logger.info("Evicted {} cache keys matching pattern: {}", deletedCount, pattern);
            } else {
                logger.debug("No cache keys found matching pattern: {}", pattern);
            }
        } catch (RedisConnectionFailureException e) {
            logger.warn("Redis connection failed while evicting pattern: {}. Degrading gracefully.", pattern, e);
        } catch (Exception e) {
            logger.error("Unexpected error while evicting cache pattern: {}", pattern, e);
        }
    }

    public void evictAll() {
        if (!redisEnabled) {
            logger.debug("Cache is disabled, skipping flush all");
            return;
        }

        try {
            redisTemplate.getConnectionFactory().getConnection().flushDb();
            logger.warn("Flushed all cache entries from current database");
        } catch (RedisConnectionFailureException e) {
            logger.warn("Redis connection failed while flushing database. Degrading gracefully.", e);
        } catch (Exception e) {
            logger.error("Unexpected error while flushing cache database", e);
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
            logger.warn("Redis connection failed while checking key existence: {}. Degrading gracefully.", key, e);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error while checking cache key existence: {}", key, e);
            return false;
        }
    }

    public void setExpire(String key, Duration ttl) {
        if (!redisEnabled) {
            logger.debug("Cache is disabled, skipping TTL update for key: {}", key);
            return;
        }

        try {
            Boolean updated = redisTemplate.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS);
            if (Boolean.TRUE.equals(updated)) {
                logger.debug("Updated TTL for key: {} to {}", key, ttl);
            } else {
                logger.debug("Failed to update TTL for key: {} (key may not exist)", key);
            }
        } catch (RedisConnectionFailureException e) {
            logger.warn("Redis connection failed while updating TTL for key: {}. Degrading gracefully.", key, e);
        } catch (Exception e) {
            logger.error("Unexpected error while updating TTL for cache key: {}", key, e);
        }
    }

    public Long increment(String key) {
        return increment(key, 1L);
    }

    public Long increment(String key, long delta) {
        if (!redisEnabled) {
            logger.debug("Cache is disabled, skipping increment for key: {}", key);
            return 0L;
        }

        try {
            Long result = redisTemplate.opsForValue().increment(key, delta);
            logger.debug("Incremented key: {} by {} to {}", key, delta, result);
            return result != null ? result : 0L;
        } catch (RedisConnectionFailureException e) {
            logger.warn("Redis connection failed while incrementing key: {}. Degrading gracefully.", key, e);
            return 0L;
        } catch (Exception e) {
            logger.error("Unexpected error while incrementing cache key: {}", key, e);
            return 0L;
        }
    }

    public Long decrement(String key) {
        return decrement(key, 1L);
    }

    public Long decrement(String key, long delta) {
        if (!redisEnabled) {
            logger.debug("Cache is disabled, skipping decrement for key: {}", key);
            return 0L;
        }

        try {
            Long result = redisTemplate.opsForValue().decrement(key, delta);
            logger.debug("Decremented key: {} by {} to {}", key, delta, result);
            return result != null ? result : 0L;
        } catch (RedisConnectionFailureException e) {
            logger.warn("Redis connection failed while decrementing key: {}. Degrading gracefully.", key, e);
            return 0L;
        } catch (Exception e) {
            logger.error("Unexpected error while decrementing cache key: {}", key, e);
            return 0L;
        }
    }
}
