package com.agriconnect.Contract.exception;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.stereotype.Component;

@Aspect
@Component
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true", matchIfMissing = false)
public class CacheExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(CacheExceptionHandler.class);

    @Around("@annotation(org.springframework.cache.annotation.Cacheable) || " +
            "@annotation(org.springframework.cache.annotation.CachePut) || " +
            "@annotation(org.springframework.cache.annotation.CacheEvict)")
    public Object handleCacheExceptions(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (RedisConnectionFailureException e) {
            logger.warn("Redis connection failed during cache operation. Degrading gracefully: {}", e.getMessage());
            return joinPoint.proceed();
        } catch (RedisSystemException e) {
            logger.warn("Redis system error during cache operation. Degrading gracefully: {}", e.getMessage());
            return joinPoint.proceed();
        } catch (Exception e) {
            if (e.getCause() instanceof RedisConnectionFailureException) {
                logger.warn("Redis connection failed (nested). Degrading gracefully: {}", e.getMessage());
                return joinPoint.proceed();
            }
            throw e;
        }
    }
}
