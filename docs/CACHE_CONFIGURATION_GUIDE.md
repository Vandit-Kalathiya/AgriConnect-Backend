# Cache Configuration Guide - Redis (Local) vs Valkey (Production)

## Overview

The caching system is designed with **separate feature flags** for local and production environments:

- **Local Development**: Uses **Redis** via Docker Compose
- **Production**: Uses **Valkey** on AWS ElastiCache

This separation allows you to test with Redis locally and deploy with Valkey in production without any code changes.

---

## 🏠 Local Development (Redis)

### 1. Enable Redis Caching

In your `.env` file:

```properties
# Enable Redis for local development
CACHE_LOCAL_ENABLED=true

# Redis connection (Docker Compose)
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=
```

### 2. Start Redis

```bash
# Start Redis container
docker-compose up -d redis

# Verify Redis is running
docker-compose ps redis
```

### 3. Run Application

```bash
./mvnw spring-boot:run
```

### 4. Verify Caching

```bash
# Check health
curl http://localhost:8080/actuator/health

# Should show Redis status
{
  "status": "UP",
  "components": {
    "redis": {
      "status": "UP",
      "details": {
        "service": "Redis/Valkey",
        "status": "Connected"
      }
    }
  }
}
```

### 5. Test Redis CLI

```bash
# Connect to Redis
docker exec -it agriconnect-redis redis-cli

# Test commands
127.0.0.1:6379> PING
PONG

# View cached OTPs
127.0.0.1:6379> KEYS otp:*

# View specific key
127.0.0.1:6379> GET "otp:email:registration:user@example.com"
```

---

## 🚀 Production (Valkey on AWS ElastiCache)

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
  --transit-encryption-enabled
```

### 2. Configure Security Group

Allow inbound TCP 6379 from your EC2 security group.

### 3. Get Valkey Endpoint

```bash
aws elasticache describe-cache-clusters \
  --cache-cluster-id agriconnect-valkey \
  --show-cache-node-info \
  --query 'CacheClusters[0].CacheNodes[0].Endpoint'
```

### 4. Set Environment Variables (Production EC2)

```bash
# Enable Valkey caching
export CACHE_PROD_ENABLED=true

# Valkey connection (AWS ElastiCache endpoint)
export VALKEY_URL="rediss://agriconnect-valkey.xxxxxx.cache.amazonaws.com:6379"
export VALKEY_PASSWORD="your-secure-password"  # If using AUTH

# Disable local Redis
export CACHE_LOCAL_ENABLED=false
```

### 5. Deploy Application

```bash
# Set profile to prod
export SPRING_PROFILES_ACTIVE=prod

# Run application
java -jar target/Api-Gateway-0.0.1-SNAPSHOT.jar
```

---

## 🎛️ Feature Flag Matrix

| Environment | Profile | CACHE_LOCAL_ENABLED | CACHE_PROD_ENABLED | Cache Engine | Connection |
|-------------|---------|---------------------|--------------------| -------------|------------|
| **Local Dev** | `dev` | `true` | `false` | Redis | `REDIS_HOST:REDIS_PORT` |
| **Production** | `prod` | `false` | `true` | Valkey | `VALKEY_URL` |
| **No Cache** | any | `false` | `false` | None | App works normally |

---

## 📝 Configuration Files

### application.yml (Dev Profile)

```yaml
cache:
  local:
    enabled: ${CACHE_LOCAL_ENABLED:false}  # Redis
  prod:
    enabled: ${CACHE_PROD_ENABLED:false}   # Valkey

redis:
  enabled: ${CACHE_LOCAL_ENABLED:false}

spring:
  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
```

### application.yml (Prod Profile)

```yaml
spring:
  config:
    activate:
      on-profile: prod
  data:
    redis:
      url: ${VALKEY_URL}  # Valkey endpoint
      password: ${VALKEY_PASSWORD:}
      ssl:
        enabled: true

cache:
  local:
    enabled: false  # Disable Redis
  prod:
    enabled: ${CACHE_PROD_ENABLED:true}  # Enable Valkey

redis:
  enabled: ${CACHE_PROD_ENABLED:true}
```

---

## 🔄 Switching Between Redis and Valkey

### Scenario 1: Local Development with Redis

```bash
# .env file
CACHE_LOCAL_ENABLED=true
CACHE_PROD_ENABLED=false
REDIS_HOST=127.0.0.1
REDIS_PORT=6379

# Start Redis
docker-compose up -d redis

# Run app
./mvnw spring-boot:run
```

### Scenario 2: Production with Valkey

```bash
# EC2 environment variables
export CACHE_LOCAL_ENABLED=false
export CACHE_PROD_ENABLED=true
export VALKEY_URL="rediss://your-cluster.cache.amazonaws.com:6379"
export SPRING_PROFILES_ACTIVE=prod

# Run app
java -jar app.jar
```

### Scenario 3: No Caching (Testing/Debugging)

```bash
# Disable both
export CACHE_LOCAL_ENABLED=false
export CACHE_PROD_ENABLED=false

# App works normally without cache
./mvnw spring-boot:run
```

---

## 🧪 Testing Scenarios

### Test 1: Local Redis Caching

```bash
# Enable Redis
export CACHE_LOCAL_ENABLED=true
docker-compose up -d redis

# Run app and test OTP caching
curl -X POST http://localhost:8080/api/auth/send-otp \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com"}'

# Check Redis
docker exec -it agriconnect-redis redis-cli
127.0.0.1:6379> KEYS otp:*
```

### Test 2: Production Valkey Caching

```bash
# On EC2 with Valkey configured
export CACHE_PROD_ENABLED=true
export VALKEY_URL="rediss://your-cluster.cache.amazonaws.com:6379"

# Check health
curl http://localhost:8080/actuator/health

# Should show Valkey connection
```

### Test 3: Graceful Degradation

```bash
# Stop Redis while app is running
docker-compose stop redis

# App should continue working (logs will show cache warnings)
# Requests still succeed, just without caching
```

---

## 📊 Monitoring

### Health Check

```bash
# Local (Redis)
curl http://localhost:8080/actuator/health | jq '.components.redis'

# Production (Valkey)
curl http://your-domain.com/actuator/health | jq '.components.redis'
```

### Logs

```bash
# Look for cache initialization
grep "CacheService initialized" logs/application.log

# Check cache operations
grep "cached:" logs/application.log

# Monitor Redis/Valkey connections
grep "Redis" logs/application.log
```

### Metrics

```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus | grep cache

# Key metrics:
# - cache_gets_total
# - cache_puts_total
# - cache_hits_total
# - cache_misses_total
```

---

## 🛠️ Troubleshooting

### Issue: "Redis connection failed"

**Local Development:**
```bash
# Check if Redis is running
docker-compose ps redis

# Restart Redis
docker-compose restart redis

# Check logs
docker-compose logs redis
```

**Production:**
```bash
# Check Valkey endpoint
echo $VALKEY_URL

# Test connection from EC2
redis-cli -h your-cluster.cache.amazonaws.com -p 6379 --tls ping

# Check security group rules
aws ec2 describe-security-groups --group-ids sg-xxxxxx
```

### Issue: "Cache not working"

```bash
# Check feature flags
echo $CACHE_LOCAL_ENABLED
echo $CACHE_PROD_ENABLED

# Check application logs
grep "redis.enabled" logs/application.log

# Verify profile
echo $SPRING_PROFILES_ACTIVE
```

### Issue: "Wrong cache engine in production"

```bash
# Make sure local cache is disabled in prod
export CACHE_LOCAL_ENABLED=false
export CACHE_PROD_ENABLED=true

# Verify Valkey URL is set
echo $VALKEY_URL
```

---

## 🔐 Security Best Practices

### Local Development

- ✅ Use empty password for Redis (Docker Compose)
- ✅ Redis only accessible on localhost
- ✅ No TLS needed for local development

### Production

- ✅ Always use TLS (`rediss://` scheme)
- ✅ Set strong password for Valkey
- ✅ Restrict security group to EC2 instances only
- ✅ Use IAM roles for AWS credentials
- ✅ Never commit `VALKEY_URL` or `VALKEY_PASSWORD` to git

---

## 📋 Environment Variables Summary

### Required for Local Development

```properties
CACHE_LOCAL_ENABLED=true
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=
```

### Required for Production

```properties
CACHE_PROD_ENABLED=true
VALKEY_URL=rediss://your-cluster.cache.amazonaws.com:6379
VALKEY_PASSWORD=your-secure-password
SPRING_PROFILES_ACTIVE=prod
```

### Optional (Both Environments)

```properties
# Disable all caching
CACHE_LOCAL_ENABLED=false
CACHE_PROD_ENABLED=false
```

---

## 🎯 Quick Commands

```bash
# Local: Start Redis and enable caching
docker-compose up -d redis
export CACHE_LOCAL_ENABLED=true
./mvnw spring-boot:run

# Production: Enable Valkey
export CACHE_PROD_ENABLED=true
export VALKEY_URL="rediss://your-cluster.cache.amazonaws.com:6379"
export SPRING_PROFILES_ACTIVE=prod
java -jar app.jar

# Disable caching (any environment)
export CACHE_LOCAL_ENABLED=false
export CACHE_PROD_ENABLED=false

# Check health
curl http://localhost:8080/actuator/health

# View Redis keys (local)
docker exec -it agriconnect-redis redis-cli KEYS '*'
```

---

## ✅ Deployment Checklist

### Local Development Setup
- [ ] Docker Compose installed
- [ ] Redis service in docker-compose.yml
- [ ] `CACHE_LOCAL_ENABLED=true` in .env
- [ ] Redis container running
- [ ] Health check shows Redis UP

### Production Deployment
- [ ] AWS ElastiCache Valkey cluster created
- [ ] TLS encryption enabled
- [ ] Security group configured (port 6379)
- [ ] `VALKEY_URL` environment variable set on EC2
- [ ] `VALKEY_PASSWORD` set (if using AUTH)
- [ ] `CACHE_PROD_ENABLED=true` on EC2
- [ ] `CACHE_LOCAL_ENABLED=false` on EC2
- [ ] `SPRING_PROFILES_ACTIVE=prod` on EC2
- [ ] Health check shows Valkey UP
- [ ] Tested graceful degradation

---

**Last Updated**: April 2026  
**Version**: 2.0.0 (Separate Redis/Valkey Support)  
**Maintainer**: AgriConnect Backend Team
