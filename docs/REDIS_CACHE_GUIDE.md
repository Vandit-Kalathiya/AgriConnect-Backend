# Redis/Valkey Caching Implementation Guide

## Overview

This application uses **Redis/Valkey** for distributed caching with AWS ElastiCache support. The caching layer is production-ready with:

- ✅ Feature flag control (`redis.enabled`)
- ✅ Graceful degradation on failures
- ✅ Per-cache TTL configuration
- ✅ Professional CacheService API
- ✅ Health monitoring
- ✅ Zero-downtime rollback

## Architecture

### Components

1. **RedisConfig** - Main configuration with Lettuce client and connection pooling
2. **NoOpCacheConfig** - Fallback when caching is disabled
3. **CacheService** - Professional wrapper for easy cache operations
4. **RedisHealthIndicator** - Health check for monitoring
5. **CacheExceptionHandler** - Graceful error handling

### Cache Names & TTLs

| Cache Name | TTL | Purpose |
|------------|-----|---------|
| `userCache` | 7 days | User authentication data |
| `otpCache` | 5 minutes | Email/SMS OTP codes |
| `userProfileCache` | 1 hour | User profile information |
| `sessionCache` | 7 days | Session validation |
| `tokenCache` | 7 days | JWT token metadata |

## Environment Variables

### Development (.env)

```properties
# Enable/disable caching
REDIS_ENABLED=true

# Redis connection (local Docker)
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=

# Or use connection URL
# REDIS_URL=redis://localhost:6379
```

### Production (AWS EC2)

```properties
# Enable caching in production
REDIS_ENABLED=true

# AWS ElastiCache Valkey endpoint with TLS
REDIS_URL=rediss://your-cluster.cache.amazonaws.com:6379

# Optional: If using password authentication
REDIS_PASSWORD=your-secure-password
```

## Using CacheService (Recommended)

The `CacheService` provides a clean, professional API for all cache operations:

### Basic Operations

```java
@Service
public class YourService {
    
    private final CacheService cacheService;
    
    @Autowired
    public YourService(CacheService cacheService) {
        this.cacheService = cacheService;
    }
    
    // Save with default TTL (1 hour)
    public void saveData(String key, MyObject data) {
        cacheService.save(key, data);
    }
    
    // Save with custom TTL
    public void saveOtp(String email, String otp) {
        String key = "otp:email:" + email;
        cacheService.save(key, otp, Duration.ofMinutes(5));
    }
    
    // Get cached data
    public Optional<MyObject> getData(String key) {
        return cacheService.get(key, MyObject.class);
    }
    
    // Check if key exists
    public boolean hasData(String key) {
        return cacheService.exists(key);
    }
    
    // Evict single key
    public void clearData(String key) {
        cacheService.evict(key);
    }
    
    // Evict by pattern (e.g., all OTPs for a user)
    public void clearUserOtps(String userId) {
        cacheService.evictPattern("otp:*:" + userId);
    }
    
    // Update TTL
    public void extendSession(String sessionId) {
        cacheService.setExpire("session:" + sessionId, Duration.ofDays(7));
    }
    
    // Increment counter
    public Long incrementLoginAttempts(String userId) {
        return cacheService.increment("login:attempts:" + userId);
    }
}
```

### Using Spring @Cacheable Annotations

```java
@Service
public class UserService {
    
    // Cache user profile for 1 hour
    @Cacheable(value = "userProfileCache", key = "#userId")
    public User getUserProfile(Long userId) {
        // This will only execute if not in cache
        return userRepository.findById(userId).orElse(null);
    }
    
    // Update cache on profile update
    @CachePut(value = "userProfileCache", key = "#user.id")
    public User updateUserProfile(User user) {
        return userRepository.save(user);
    }
    
    // Evict cache on deletion
    @CacheEvict(value = "userProfileCache", key = "#userId")
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
    
    // Evict all user profiles
    @CacheEvict(value = "userProfileCache", allEntries = true)
    public void clearAllUserProfiles() {
        // Cache cleared before this executes
    }
}
```

## Cache Key Patterns

Use consistent key patterns for easy management:

```
# User data
user:{userId}
user:profile:{userId}
user:session:{sessionId}

# OTP codes
otp:email:{email}
otp:phone:{phoneNumber}

# Authentication
auth:token:{tokenId}
auth:attempts:{userId}

# Rate limiting
ratelimit:{endpoint}:{userId}
```

## AWS ElastiCache Setup

### 1. Create Valkey Cluster

```bash
# Using AWS CLI
aws elasticache create-cache-cluster \
  --cache-cluster-id agriconnect-valkey \
  --engine valkey \
  --engine-version 7.2 \
  --cache-node-type cache.t3.micro \
  --num-cache-nodes 1 \
  --security-group-ids sg-xxxxxxxxx \
  --subnet-group-name your-subnet-group \
  --transit-encryption-enabled
```

### 2. Configure Security Group

Allow inbound traffic from EC2 instances:
- **Type**: Custom TCP
- **Port**: 6379
- **Source**: EC2 security group

### 3. Get Endpoint

```bash
aws elasticache describe-cache-clusters \
  --cache-cluster-id agriconnect-valkey \
  --show-cache-node-info
```

### 4. Set Environment Variable

```bash
export REDIS_URL="rediss://agriconnect-valkey.xxxxxx.cache.amazonaws.com:6379"
export REDIS_ENABLED=true
```

## Local Development

### Start Redis with Docker Compose

```bash
# Start all services including Redis
docker-compose up -d

# Or start only Redis
docker-compose up -d redis

# Check Redis is running
docker-compose ps redis

# View Redis logs
docker-compose logs -f redis
```

### Connect to Redis CLI

```bash
# Connect to local Redis
docker exec -it agriconnect-redis redis-cli

# Test connection
127.0.0.1:6379> PING
PONG

# View all keys
127.0.0.1:6379> KEYS *

# Get a specific key
127.0.0.1:6379> GET "user:123"

# Check TTL
127.0.0.1:6379> TTL "otp:email:user@example.com"
```

## Monitoring

### Health Check

```bash
# Check cache health
curl http://localhost:8080/actuator/health

# Response when Redis is UP:
{
  "status": "UP",
  "components": {
    "redis": {
      "status": "UP",
      "details": {
        "service": "Redis/Valkey",
        "status": "Connected",
        "response": "PONG"
      }
    }
  }
}
```

### Metrics

Cache metrics are exposed via Micrometer:

```bash
# View Prometheus metrics
curl http://localhost:8080/actuator/prometheus | grep cache

# Key metrics:
# - cache_gets_total
# - cache_puts_total
# - cache_evictions_total
# - cache_hits_total
# - cache_misses_total
```

## Troubleshooting

### Cache Not Working

1. **Check if enabled**:
   ```bash
   echo $REDIS_ENABLED
   # Should output: true
   ```

2. **Check Redis connection**:
   ```bash
   # Test connection
   redis-cli -h $REDIS_HOST -p $REDIS_PORT ping
   ```

3. **Check application logs**:
   ```bash
   # Look for cache initialization
   grep "CacheService initialized" logs/application.log
   ```

### Graceful Degradation

If Redis fails, the application continues working without cache:

```
2026-04-20 20:00:00 - WARN - Redis connection failed while getting key: user:123. Degrading gracefully.
```

### Disable Caching

To disable caching without code changes:

```bash
# Set environment variable
export REDIS_ENABLED=false

# Restart application
./mvnw spring-boot:run
```

## Performance Tips

### 1. Use Appropriate TTLs

- **Short TTL** (5 min): OTP codes, temporary data
- **Medium TTL** (1 hour): User profiles, frequently changing data
- **Long TTL** (7 days): Sessions, rarely changing data

### 2. Cache Key Design

```java
// ❌ Bad: Hard to manage
cache.save("abc123", data);

// ✅ Good: Structured and searchable
cache.save("user:profile:" + userId, data);
```

### 3. Batch Operations

```java
// ❌ Bad: Multiple round trips
for (String key : keys) {
    cache.evict(key);
}

// ✅ Good: Single pattern eviction
cache.evictPattern("user:profile:*");
```

### 4. Monitor Cache Hit Ratio

Aim for >70% cache hit ratio:

```java
// Track in your service
long hits = cacheService.get(key, Type.class).isPresent() ? hits++ : misses++;
double hitRatio = (double) hits / (hits + misses);
```

## Testing

### Unit Tests

```java
@SpringBootTest
@TestPropertySource(properties = {"redis.enabled=false"})
class YourServiceTest {
    
    @Test
    void testWithoutCache() {
        // Test works even when cache is disabled
    }
}
```

### Integration Tests

```java
@SpringBootTest
@TestPropertySource(properties = {"redis.enabled=true"})
class CacheIntegrationTest {
    
    @Autowired
    private CacheService cacheService;
    
    @Test
    void testCacheOperations() {
        cacheService.save("test:key", "value");
        Optional<String> result = cacheService.get("test:key", String.class);
        assertTrue(result.isPresent());
        assertEquals("value", result.get());
    }
}
```

## Production Checklist

- [ ] AWS ElastiCache Valkey cluster created
- [ ] TLS encryption enabled
- [ ] Security group configured
- [ ] `REDIS_URL` environment variable set
- [ ] `REDIS_ENABLED=true` in production
- [ ] Health check endpoint monitored
- [ ] Metrics dashboard configured
- [ ] Backup/snapshot policy configured
- [ ] Tested graceful degradation
- [ ] Documented rollback procedure

## Rollback Procedure

If caching causes issues:

1. **Disable cache** (zero downtime):
   ```bash
   export REDIS_ENABLED=false
   # Restart application
   ```

2. **Application continues** working without cache

3. **Investigate** Redis issues offline

4. **Re-enable** when resolved:
   ```bash
   export REDIS_ENABLED=true
   # Restart application
   ```

## Support

For issues or questions:
1. Check application logs
2. Verify environment variables
3. Test Redis connection
4. Review this guide
5. Contact DevOps team

---

**Last Updated**: April 2026  
**Version**: 1.0.0  
**Maintainer**: AgriConnect Backend Team
