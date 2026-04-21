# AgriConnect Backend Documentation

Comprehensive technical documentation for the AgriConnect platform.

---

## 📚 Core Guides

### **[CACHING_GUIDE.md](CACHING_GUIDE.md)**

Redis/Valkey distributed caching with 85-98% performance improvement. Covers setup, strategy, implementation, and troubleshooting.

### **[PERFORMANCE_OPTIMIZATION.md](PERFORMANCE_OPTIMIZATION.md)**

Database indexes and query optimization achieving 85-98% faster queries. Includes metrics, best practices, and monitoring.

### **[DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)**

Feature flag-based deployment with zero-downtime releases. Covers environment setup, rollback procedures, and health checks.

### **[AI_FEATURES_GUIDE.md](AI_FEATURES_GUIDE.md)**

AI chat, crop advisory, and Kisan Mitra features. Includes API endpoints, database schema, and UI integration.

### **[NOTIFICATIONS_GUIDE.md](NOTIFICATIONS_GUIDE.md)**

Multi-channel notification system (email, SMS, in-app) with Kafka. Covers 28 event scenarios, reliability patterns, and monitoring.

---

## 📋 Reference Documents

### **[NOTIFICATION_SCENARIOS.md](NOTIFICATION_SCENARIOS.md)**

Complete list of 28 notification trigger scenarios across all services.

### **[PRODUCTION_CHECKLIST.md](PRODUCTION_CHECKLIST.md)**

Pre-deployment and post-deployment verification checklist.

---

## 🚀 Quick Start

```bash
# 1. Setup environment
cp .env.example .env

# 2. Start dependencies
docker-compose up -d redis postgres kafka

# 3. Run application
./mvnw spring-boot:run

# 4. Verify health
curl http://localhost:8080/actuator/health
```

---

## 🎯 Key Features

**Performance:**

- 85-98% faster queries with database indexes
- 95%+ cache hit rate with Redis/Valkey
- Sub-100ms response times

**Reliability:**

- Feature flag control for all services
- Graceful degradation on failures
- Zero-downtime deployments
- Kafka-based event processing

**Scalability:**

- Distributed caching
- Cursor-based pagination
- Connection pooling
- Async notification delivery

---

## 🔧 Configuration

### Environment Variables

```properties
# Caching
CACHE_LOCAL_ENABLED=true
CACHE_PRODUCTION_ENABLED=true

# Notifications
NOTIFICATION_ENABLED=true
NOTIFICATION_EMAIL_ENABLED=true

# AI Features
AI_ENABLED=true
AI_CHAT_ENABLED=true
```

### Health Endpoints

```
GET /actuator/health
GET /actuator/health/redis
GET /actuator/health/db
```

---

## 📊 Performance Metrics

| Metric           | Before | After | Improvement |
| ---------------- | ------ | ----- | ----------- |
| Active Listings  | 450ms  | 65ms  | 85% faster  |
| User Auth        | 120ms  | 6ms   | 95% faster  |
| Order Pagination | 1200ms | 120ms | 90% faster  |
| Agreement Lookup | 250ms  | 5ms   | 98% faster  |

---

## 🔗 Quick Links

- **Main README:** [../README.md](../README.md)
- **API Documentation:** [Swagger UI](http://localhost:8080/swagger-ui.html)
- **Health Dashboard:** [Actuator](http://localhost:8080/actuator)
