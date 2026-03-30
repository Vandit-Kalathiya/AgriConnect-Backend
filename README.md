# AgriConnect Backend

AgriConnect Backend is a microservices platform powering agricultural workflows across farmer onboarding, market access, contract farming, agreement generation, and AI-assisted advisory.

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

---

## Service Boundaries

- `Api-Gateway`: single entry point, routing, resiliency, CORS, unified API docs
- `Eureka-Main-Server`: service registry and discovery
- `Main-Backend`: authentication, users, identity-related domain logic
- `Contract-Farming-App`: contract, order, payment-related workflows
- `Market-Access-App`: listings, marketplace operations, AI orchestration and AI persistence
- `Generate-Agreement-App`: agreement and document-generation capabilities

All client integrations should call APIs through the gateway route space.

---

## Technology Stack

- Spring Boot and Spring Cloud ecosystem
- Spring Cloud Gateway and Eureka
- Spring Security with JWT
- JPA/Hibernate + Flyway migrations
- Resilience4j for fault tolerance
- OpenAPI/Swagger documentation
- Maven build pipeline
- Container-friendly deployment model

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

---

## Observability and Reliability

Platform reliability patterns include:
- circuit breaker protections at gateway boundaries
- centralized request routing and CORS policy handling
- health/metrics actuator exposure for service monitoring
- structured logs per service

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
- always use gateway-facing API paths
- keep chat and crop-advisory histories in separate UI flows
- use conversation APIs for thread-based chat UX
- handle retention cleanup outcomes gracefully (empty/stale-thread states)

Reference guides:
- `AI_HISTORY_UI_INTEGRATION_GUIDE.md`
- `API_GATEWAY_FRONTEND_MIGRATION.md`

---

## Repository Structure

```text
Backend/
├── Api-Gateway/
├── Eureka-Main-Server/
├── Main-Backend/
├── Contract-Farming-App/
├── Market-Access-App/
├── Generate-Agreement-App/
├── README.md
├── QUICK_START.md
├── PRODUCTION_DB_MIGRATION_GUIDE.md
├── AI_HISTORY_UI_INTEGRATION_GUIDE.md
└── API_GATEWAY_FRONTEND_MIGRATION.md
```

---

## Quick Start

For setup and local run instructions, use:
- `QUICK_START.md`

For AI history UI integration details, use:
- `AI_HISTORY_UI_INTEGRATION_GUIDE.md`

For production DB migration procedures, use:
- `PRODUCTION_DB_MIGRATION_GUIDE.md`

---

## License

[Project License Placeholder]

---

**Last Updated**: March 2026  
**Version**: 2.1.0
