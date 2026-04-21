# Notifications Guide

## Overview
Multi-channel notification system with email, SMS, and in-app delivery using Kafka for reliable event processing.

---

## Architecture

**Components:**
- **Producer Services** - Emit notification events to Kafka
- **Notification Service** - Consumes events and dispatches notifications
- **Delivery Channels** - Email (SendGrid), SMS (Twilio), In-App
- **Outbox Pattern** - Ensures at-least-once delivery

**Flow:**
```
Service → Kafka Topic → Notification Service → Delivery Channels → User
```

---

## Notification Types

### 28 Event Scenarios

**Authentication:**
- Registration OTP, Login OTP, Password reset

**Marketplace:**
- New listing, Listing sold, Price update, Listing expired

**Orders:**
- Order placed, Payment confirmed, Order shipped, Order delivered

**Agreements:**
- Agreement created, Agreement signed, Agreement expired

**Cold Storage:**
- Booking confirmed, Storage reminder, Booking expired

**AI Features:**
- Advisory ready, Chat response, Recommendation available

---

## API Endpoints

### Send Notification
```http
POST /api/notifications/send
{
  "userId": "user-id",
  "type": "ORDER_PLACED",
  "channel": "EMAIL",
  "data": { "orderId": "123" }
}
```

### Get Notifications
```http
GET /api/notifications?page=0&size=20
GET /api/notifications/unread/count
```

### Mark as Read
```http
PUT /api/notifications/{id}/read
PUT /api/notifications/read-all
```

---

## Configuration

### Feature Flags
```properties
NOTIFICATION_ENABLED=true
NOTIFICATION_EMAIL_ENABLED=true
NOTIFICATION_SMS_ENABLED=false
NOTIFICATION_INAPP_ENABLED=true
```

### Provider Credentials
```properties
# SendGrid (Email)
SENDGRID_API_KEY=your-key
SENDGRID_FROM_EMAIL=noreply@agriconnect.com

# Twilio (SMS)
TWILIO_ACCOUNT_SID=your-sid
TWILIO_AUTH_TOKEN=your-token
TWILIO_PHONE_NUMBER=+1234567890
```

---

## Database Schema

### Tables
- `notifications` - Notification records
- `notification_delivery_log` - Delivery tracking
- `notification_outbox` - Outbox pattern for reliability

### Indexes
```java
@Index(name = "idx_notifications_user_id", columnList = "user_id")
@Index(name = "idx_notifications_status", columnList = "status")
@Index(name = "idx_delivery_log_event_id", columnList = "event_id")
```

---

## UI Integration

### Real-Time Updates
```typescript
// Fetch notifications
GET /api/notifications?page=0&size=20

// Unread count
GET /api/notifications/unread/count

// Mark as read
PUT /api/notifications/{id}/read

// WebSocket (optional)
ws://localhost:8080/ws/notifications
```

### Display Components
- Notification bell with unread count
- Dropdown list with recent notifications
- Full notification page with pagination
- Toast/snackbar for real-time alerts

---

## Kafka Topics

### Topic Structure
```
notification-events-{service}
├─ notification-events-marketplace
├─ notification-events-orders
├─ notification-events-agreements
└─ notification-events-auth
```

### Event Format
```json
{
  "eventId": "uuid",
  "userId": "user-id",
  "type": "ORDER_PLACED",
  "channel": "EMAIL",
  "timestamp": "2026-04-21T10:00:00Z",
  "data": { "orderId": "123" }
}
```

---

## Reliability

### Outbox Pattern
```java
// 1. Save to outbox table in same transaction
// 2. Background job publishes to Kafka
// 3. Mark as published after confirmation
```

### Idempotency
```java
// Delivery log prevents duplicate sends
@UniqueConstraint(columnNames = {"event_id", "channel"})
```

### Retry Logic
- Failed deliveries retry with exponential backoff
- Max 3 retry attempts
- Dead letter queue for permanent failures

---

## Monitoring

### Metrics
- Delivery success rate (target: >99%)
- Average delivery time (target: <5s)
- Failed deliveries (target: <1%)
- Kafka lag (target: <100 messages)

### Commands
```bash
# Check Kafka lag
kafka-consumer-groups --describe --group notification-service

# View failed notifications
SELECT * FROM notifications WHERE status = 'FAILED';

# Delivery stats
SELECT channel, status, COUNT(*) FROM notification_delivery_log GROUP BY channel, status;
```

---

## Best Practices

**DO:**
- ✅ Use feature flags for gradual rollout
- ✅ Implement idempotency checks
- ✅ Log all delivery attempts
- ✅ Handle provider rate limits

**DON'T:**
- ❌ Send notifications synchronously
- ❌ Skip error handling
- ❌ Ignore delivery failures
- ❌ Forget to test all channels

---

## Related Files
- `NotificationService.java` - Main service
- `NotificationRepository.java` - Data access
- `KafkaNotificationConsumer.java` - Event consumer
