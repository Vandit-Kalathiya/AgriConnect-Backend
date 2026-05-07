# AgriConnect Cached APIs - Quick Test Guide

## Login First
```bash
curl -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"9876543210","password":"password123"}'
```

---

## 📋 All Cached APIs by Service

### 1. API Gateway - User Data (12h TTL)
- `GET /users/{phone}` - Cache Key: `user:phone:{phone}`
- `GET /users/unique/{id}` - Cache Key: `user:unique:{id}`
- `GET /users/profile-image/{id}` - Cache Key: `user:profile:image:{id}` (24h)
- `GET /users/signature-image/{id}` - Cache Key: `user:signature:image:{id}` (24h)

### 2. Market-Access-App - Listings (30min - 2h TTL)
- `GET /listings/get/{id}` - Cache Key: `listing:{id}` (2h)
- `GET /listings/all` - Cache Key: `listings:all` (30min)
- `GET /listings/all/active` - Cache Key: `listings:active` (30min)
- `GET /listings/user/{contact}` - Cache Key: `listings:farmer:{contact}` (1h)
- `GET /listings/{id}/image` - Cache Key: `listing:images:{id}` (24h)

### 3. Market-Access-App - AI (1h TTL, requires X-User-Phone header)
- `POST /market/api/v1/ai/market/crop-analysis`
- `POST /market/api/v1/ai/crop/recommendations`
- `POST /market/api/v1/ai/market/recommendations`
- `POST /market/api/v1/ai/listing/shelf-life`
- `POST /market/api/v1/ai/listing/price-suggestion`

### 4. Notification-Service (2min - 5min TTL)
- `GET /api/notifications?userId=&channel=&page=0&size=20` (2min)
- `GET /api/notifications/unread-count?userId=` (30sec)
- `GET /api/notifications/stats` (5min)

### 5. Generate-Agreement-App - Cold Storage (1h - 2h TTL)
- `GET /coldStorage/{placeId}` (2h)
- `GET /coldStorage/nearby?lat=&lon=` (1h)
- `GET /coldStorage/nearby/d/s?district=&state=&lat=&lon=` (1h)

---

## 🧪 Test Commands

### Test Listing Cache (Fastest)
```bash
# First call - MISS
curl -X GET "http://localhost:8080/listings/all"
# Second call - HIT (should be much faster)
curl -X GET "http://localhost:8080/listings/all"
```

### Test User Cache
```bash
curl -X GET "http://localhost:8080/users/9876543210"
curl -X GET "http://localhost:8080/users/9876543210"
```

### Test Notification Cache
```bash
curl -X GET "http://localhost:8091/api/notifications?userId=user123&channel=IN_APP&page=0&size=20"
curl -X GET "http://localhost:8091/api/notifications?userId=user123&channel=IN_APP&page=0&size=20"
```

### Test AI Cache (with X-User-Phone)
```bash
curl -X POST "http://localhost:8080/market/api/v1/ai/crop/recommendations" \
  -H "X-User-Phone: 9876543210" \
  -H "Content-Type: application/json" \
  -d '{"district":"Amritsar","state":"Punjab","soilType":"Loamy","season":"Kharif","language":"en"}'
```

---

## 📊 Monitor Cache in Redis
```bash
# Enter Redis
docker exec -it redis_container redis-cli

# View all keys
KEYS *

# Check specific key
GET "listings:all"

# Check TTL (seconds)
TTL "listings:all"

# Clear all cache
FLUSHALL
```

---

## ✅ Quick Facts

- **Listing: 30min** - Gets cached fast, test after 31min
- **User: 12h** - Lasts whole day
- **Notifications: 2min** - Shortest TTL, good for testing
- **Images: 24h** - Binary data cached
- **AI: 1h** - Only with same parameters


