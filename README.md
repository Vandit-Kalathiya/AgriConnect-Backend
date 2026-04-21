# AgriConnect Backend

AgriConnect Backend is a microservices platform powering agricultural workflows across farmer onboarding, market access, contract farming, agreement generation, AI-assisted advisory, and real-time event-driven notifications.

This README is intentionally product-focused and safe for sharing:

- no hardcoded infrastructure ports
- no raw local URLs
- no secret or sensitive configuration values

---

## Product Overview

AgriConnect Backend provides:

- unified gateway-based API access for frontend clients
- secure authentication and logout lifecycle
- marketplace and listing operations
- contract lifecycle and agreement generation flows
- AI-powered assistance for chat, crop advisory, market analysis, and listing support
- persistent conversation history with retention and cleanup controls
- **event-driven real-time notification system** (Kafka + WebSocket) across all microservices
- multi-channel notification delivery (Email, SMS, Push, In-App)

---

## Service Boundaries

| Service                  | Responsibility                                                                                    |
| ------------------------ | ------------------------------------------------------------------------------------------------- |
| `Api-Gateway`            | Single entry point, routing, resiliency, CORS, JWT auth, unified API docs                         |
| `Eureka-Main-Server`     | Service registry and discovery                                                                    |
| `Market-Access-App`      | Listings, marketplace operations, AI orchestration, AI persistence                                |
| `Contract-Farming-App`   | Contract, order, payment-related workflows                                                        |
| `Generate-Agreement-App` | Agreement and document-generation capabilities                                                    |
| `Notification-Service`   | Kafka event consumer, multi-channel dispatch, in-app persistence, WebSocket push                  |
| `Ws-Gateway`             | Dedicated reactive WebSocket gateway — proxies native `ws://` connections to Notification-Service |

All client integrations should call APIs through the gateway route space.  
WebSocket connections go through the dedicated `Ws-Gateway`.

---

## Technology Stack

- Spring Boot and Spring Cloud ecosystem
- Spring Cloud Gateway (MVC for HTTP routing, **Reactive for WebSocket proxying**)
- Spring Eureka and Spring Cloud LoadBalancer
- Spring Security with JWT
- JPA/Hibernate + Flyway migrations
- Resilience4j for fault tolerance
- OpenAPI/Swagger documentation
- Maven build pipeline
- **Apache Kafka** — event streaming backbone
- **Confluent Schema Registry + Apache Avro** — schema-enforced event contracts
- **Spring Kafka + Spring WebSocket (STOMP)** — event consumption and real-time delivery
- Prometheus + Grafana — metrics and dashboards
- Container-friendly deployment model (Docker Compose)

---

## Notification System

The platform uses an event-driven notification pipeline:

```
Producer microservices  →  Kafka topics  →  Notification-Service  →  Email / SMS / Push / In-App
                                                     │
                                               WebSocket (STOMP)
                                                     │
                                              Ws-Gateway (port 8081)
                                                     │
                                              Browser / UI App
```

**Kafka topics:**

| Topic                                 | Producer                             |
| ------------------------------------- | ------------------------------------ |
| `agriconnect.notifications.auth`      | Api-Gateway                          |
| `agriconnect.notifications.market`    | Market-Access-App                    |
| `agriconnect.notifications.contract`  | Contract-Farming-App                 |
| `agriconnect.notifications.agreement` | Generate-Agreement-App               |
| `agriconnect.notifications.dlq`       | Notification-Service (failed events) |

**28 notification scenarios** are implemented across all services. See `NOTIFICATION_SCENARIOS.md` for the complete reference.

UI integration guide: `NOTIFICATION_UI_GUIDE.md`

---

## AI Capabilities (Market Access)

AI orchestration in `Market-Access-App` includes:

- domain-scoped agricultural assistant behavior
- safety decisions and fallback responses
- crop advisory and market/listing support flows
- persistent history for chat and advisory experiences
- user-scoped history retrieval and deletion
- scheduled retention cleanup with batched deletes

Detailed frontend integration: `AI_HISTORY_UI_INTEGRATION_GUIDE.md`

---

## Chat Session Model (Professional Threading)

The backend now supports ChatGPT-style thread continuity:

- New thread: frontend sends no `conversationId` on chat respond
- Continue thread: frontend sends existing `conversationId`
- Backend validates ownership and appends new message to same thread
- Conversation list returns one row per stable `conversationId`
- Conversation messages API returns all messages for that thread

This guarantees safe grouping and continuation behavior for UI.

---

## AI/History API Surface (High-Level)

Core capabilities exposed by AI endpoints:

- send/continue chat
- list conversations
- fetch messages of a conversation
- rename a conversation
- delete a single conversation
- fetch chat history (legacy-friendly)
- fetch crop advisory history
- delete chat history / delete all history

Refer to API docs in gateway Swagger and `AI_HISTORY_UI_INTEGRATION_GUIDE.md` for request/response contracts.

---

## Data Lifecycle and Cleanup

AI persistence is managed with:

- versioned Flyway migrations
- retention-based cleanup scheduling
- batched cleanup to reduce lock pressure
- orphan conversation cleanup support

Retention behavior is configurable through application properties and environment variables (without exposing secrets in this document).

---

## Security Notes

- API requests should be authenticated according to service policy.
- AI history operations are user-scoped (phone-based user context where applicable).
- Conversation continuation enforces ownership checks.
- Logout flow includes token invalidation and session-state cleanup.
- Notification REST endpoints require JWT authentication (enforced at API Gateway).
- WebSocket connections are authenticated via JWT cookie or STOMP connect headers.

---

## Observability and Reliability

Platform reliability patterns include:

- circuit breaker protections at gateway boundaries
- centralized request routing and CORS policy handling
- health/metrics actuator exposure for service monitoring
- structured logs per service
- Kafka consumer DLQ (dead-letter queue) for failed notification events
- Prometheus metrics with Grafana dashboards (Docker Compose stack)

---

## Database and Migration Guidance

Production-safe schema changes are handled using Flyway migration scripts in each service that owns data changes.

Use migration-first deployments:

- backup database
- deploy with Flyway enabled
- validate migration history and health checks
- avoid implicit schema mutation strategies in production

Operational runbook: `PRODUCTION_DB_MIGRATION_GUIDE.md`

---

## Frontend Integration Guidance

For frontend teams:

- always use gateway-facing API paths (`http://<gateway>/notifications/api/...`)
- WebSocket connections go through `Ws-Gateway` (separate port) — **not** through API Gateway
- keep chat and crop-advisory histories in separate UI flows
- use conversation APIs for thread-based chat UX
- handle retention cleanup outcomes gracefully (empty/stale-thread states)
- subscribe to `/topic/notifications/{userId}` for real-time notifications

Reference guides:

- `NOTIFICATION_UI_GUIDE.md` — real-time notification integration (WebSocket + REST)
- `NOTIFICATION_SCENARIOS.md` — all 28 notification triggers reference
- `AI_HISTORY_UI_INTEGRATION_GUIDE.md` — AI chat/history integration
- `AI_BACKEND_HANDOVER.md` — AI service handover notes

---

## Repository Structure

```text
Backend/
├── Api-Gateway/                  # HTTP gateway, JWT auth, routing, CORS
├── Eureka-Main-Server/           # Service registry
├── Market-Access-App/            # Listings, AI orchestration
├── Contract-Farming-App/         # Orders, payments, agreements
├── Generate-Agreement-App/       # PDF generation, cold storage
├── Notification-Service/         # Kafka consumer, multi-channel dispatch, WebSocket
├── Ws-Gateway/                   # Reactive WebSocket gateway (STOMP proxy)
├── docker-compose.yml            # Kafka, Schema Registry, Kafka UI, Prometheus, Grafana
├── .env                          # Root environment variables for Docker Compose
├── README.md
├── QUICK_START.md
└── docs/                         # Documentation
    ├── NOTIFICATION_UI_GUIDE.md
    ├── NOTIFICATION_SCENARIOS.md
    ├── AI_HISTORY_UI_INTEGRATION_GUIDE.md
    ├── AI_BACKEND_HANDOVER.md
    ├── PRODUCTION_CHECKLIST.md
    ├── PRODUCTION_DB_MIGRATION_GUIDE.md
    ├── DEPLOYMENT_FLAG_GUIDE.md
    ├── DEPLOYMENT_QUICK_REFERENCE.md
    ├── CACHE_CONFIGURATION_GUIDE.md
    ├── REDIS_CACHE_GUIDE.md
    └── REDIS_IMPLEMENTATION_SUMMARY.md
```

---

## Quick Start

For setup and local run instructions, use:

- `QUICK_START.md`

For notification system UI integration, use:

- `docs/NOTIFICATION_UI_GUIDE.md`

For all notification trigger scenarios, use:

- `docs/NOTIFICATION_SCENARIOS.md`

For AI history UI integration details, use:

- `docs/AI_HISTORY_UI_INTEGRATION_GUIDE.md`

For production DB migration procedures, use:

- `docs/PRODUCTION_DB_MIGRATION_GUIDE.md`

For CI/CD deployment control (enable/disable EC2 deployment), use:

- `docs/DEPLOYMENT_FLAG_GUIDE.md`

---

## License

[Project License Placeholder]

---

**Last Updated**: April 2026  
**Version**: 3.0.0
