# Deployment Guide

## Overview
Feature flag-based deployment control for zero-downtime releases across all environments.

---

## Feature Flags

### Environment Variables
```properties
# Caching
CACHE_LOCAL_ENABLED=true          # Redis (local)
CACHE_PRODUCTION_ENABLED=true     # Valkey (production)

# Notifications
NOTIFICATION_ENABLED=true
NOTIFICATION_EMAIL_ENABLED=true
NOTIFICATION_SMS_ENABLED=false

# AI Features
AI_ENABLED=true
AI_CHAT_ENABLED=true
AI_ADVISORY_ENABLED=true
```

---

## Deployment Strategy

### 1. Pre-Deployment
```bash
# Run tests
./mvnw clean test

# Build all services
./mvnw clean package -DskipTests

# Verify configuration
cat .env
```

### 2. Database Migration
```bash
# Backup database
pg_dump agriconnect > backup_$(date +%Y%m%d).sql

# Indexes auto-created by Hibernate on startup
# No manual migration needed
```

### 3. Deploy Services
```bash
# Start with feature flags OFF
CACHE_PRODUCTION_ENABLED=false
NOTIFICATION_ENABLED=false

# Deploy and verify health
curl http://localhost:8080/actuator/health

# Enable features gradually
CACHE_PRODUCTION_ENABLED=true
```

### 4. Rollback Plan
```bash
# Disable feature flags immediately
CACHE_PRODUCTION_ENABLED=false

# Restart services
# No database rollback needed
```

---

## Environment Setup

### Local Development
```properties
CACHE_LOCAL_ENABLED=true
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=2512
```

### Production
```properties
CACHE_PRODUCTION_ENABLED=true
VALKEY_URL=your-elasticache-endpoint
VALKEY_PASSWORD=your-secure-password
NOTIFICATION_ENABLED=true
```

---

## Health Checks

### Endpoints
```bash
# Overall health
GET /actuator/health

# Cache health
GET /actuator/health/redis

# Service-specific
GET /actuator/health/db
```

### Expected Response
```json
{
  "status": "UP",
  "components": {
    "redis": {"status": "UP"},
    "db": {"status": "UP"}
  }
}
```

---

## Monitoring

### Key Metrics
- Response times (target: <100ms)
- Cache hit rate (target: >95%)
- Database load (target: 50% reduction)
- Error rates (target: <0.1%)

### Commands
```bash
# Cache stats
redis-cli -a password INFO stats

# Database performance
psql -c "SELECT query, mean_exec_time FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;"

# Application logs
tail -f logs/application.log | grep ERROR
```

---

## Troubleshooting

### Cache Not Working
1. Check feature flag: `CACHE_PRODUCTION_ENABLED=true`
2. Verify connection: `redis-cli -h host -a password PING`
3. Check logs for connection errors

### Performance Issues
1. Verify indexes created: `SELECT indexname FROM pg_indexes`
2. Check cache hit rate: `redis-cli INFO stats`
3. Monitor slow queries: `pg_stat_statements`

### Service Failures
1. Check health endpoint
2. Review application logs
3. Disable problematic feature flag
4. Restart service

---

## Production Checklist

**Pre-Deployment:**
- [ ] All tests passing
- [ ] Database backup completed
- [ ] Feature flags configured
- [ ] Environment variables set
- [ ] Health checks verified

**Post-Deployment:**
- [ ] All services healthy
- [ ] Cache working (>95% hit rate)
- [ ] Performance improved (85%+ faster)
- [ ] No errors in logs
- [ ] Monitoring dashboards green

**Rollback Ready:**
- [ ] Feature flags documented
- [ ] Rollback procedure tested
- [ ] Database backup accessible
- [ ] Team notified

---

## Related Files
- `.env` - Environment configuration
- `application.yml` - Application properties
- `RedisConfig.java` - Cache configuration
