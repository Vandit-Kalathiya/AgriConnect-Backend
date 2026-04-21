package com.agriconnect.api.gateway.exception;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
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
            logger.warn("Redis connection failed during cache operation in {}.{}(). Degrading gracefully.",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(), e);
            return proceedWithoutCache(joinPoint);
        } catch (RedisSystemException e) {
            logger.warn("Redis system error during cache operation in {}.{}(). Degrading gracefully.",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(), e);
            return proceedWithoutCache(joinPoint);
        } catch (Exception e) {
            if (isCacheRelated(e)) {
                logger.error("Unexpected cache error in {}.{}(). Degrading gracefully.",
                        joinPoint.getSignature().getDeclaringTypeName(),
                        joinPoint.getSignature().getName(), e);
                return proceedWithoutCache(joinPoint);
            }
            throw e;
        }
    }

    private Object proceedWithoutCache(ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed();
    }

    private boolean isCacheRelated(Exception e) {
        String message = e.getMessage();
        return message != null && (
                message.contains("redis") ||
                message.contains("cache") ||
                message.contains("Redis") ||
                message.contains("Cache")
        );
    }
}
