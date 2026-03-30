# Quick Start Guide

Get the AgriConnect backend running quickly with safe, environment-agnostic steps.

This guide intentionally avoids hardcoded ports, local URLs, and sensitive values.

---

## Prerequisites

Choose one setup path:

- **Docker path (recommended):** Docker Desktop
- **Local path:** Java, Maven, and a compatible relational database

---

## Step 1: Clone the repository

```bash
git clone <repo-url>
cd Backend
```

---

## Step 2: Prepare environment files

Create service env files from templates:

```bash
# PowerShell
Copy-Item Main-Backend\.env.example           Main-Backend\.env
Copy-Item Contract-Farming-App\.env.example   Contract-Farming-App\.env
Copy-Item Market-Access-App\.env.example      Market-Access-App\.env
Copy-Item Generate-Agreement-App\.env.example Generate-Agreement-App\.env
```

```bash
# macOS / Linux
cp Main-Backend/.env.example           Main-Backend/.env
cp Contract-Farming-App/.env.example   Contract-Farming-App/.env
cp Market-Access-App/.env.example      Market-Access-App/.env
cp Generate-Agreement-App/.env.example Generate-Agreement-App/.env
```

Also review the root `.env` used by Docker Compose.

Use only placeholder values in shared docs and never commit real secrets.

---

## Step 3: Start services

### Option A: Docker Compose

```bash
docker compose up -d
docker compose logs -f
```

### Option B: Local Maven

Start services in dependency order:
1. service registry
2. business services
3. gateway

```bash
cd Eureka-Main-Server && mvn spring-boot:run
cd Main-Backend && mvn spring-boot:run
cd Contract-Farming-App && mvn spring-boot:run
cd Market-Access-App && mvn spring-boot:run
cd Generate-Agreement-App && mvn spring-boot:run
cd Api-Gateway && mvn spring-boot:run
```

---

## Step 4: Verify platform health

Validate:
- all services are registered in discovery
- gateway health endpoint is `UP`
- gateway route listing includes expected route groups
- Swagger/OpenAPI is reachable via gateway

Use your configured base URL:

```text
<GATEWAY_BASE_URL>
```

---

## Step 5: Smoke test core product flows

Use gateway-routed APIs to verify:
- authentication flow
- marketplace listing retrieval
- AI chat response
- AI conversation listing and message retrieval

High-level AI checks:
- new chat (without `conversationId`) creates thread
- continue chat (with `conversationId`) appends in same thread
- conversation list returns stable one-row-per-thread
- message list returns thread messages in order

---

## AI History / Chatbot Notes

For full UI + API contracts, use:
- `AI_HISTORY_UI_INTEGRATION_GUIDE.md`

Key implemented capabilities:
- conversation-based chat threads
- crop advisory history
- delete history (chat/all/single conversation)
- rename conversation
- retention-based scheduled cleanup

---

## Common operational commands

```bash
# Start
docker compose up -d

# Logs
docker compose logs -f
docker compose logs -f <service-name>

# Rebuild one service
docker compose up --build --no-deps -d <service-name>

# Restart one service
docker compose restart <service-name>

# Stop stack
docker compose down
```

---

## Troubleshooting checklist

- Service missing in registry: verify startup order and registration config.
- Gateway 5xx: verify downstream service health and registration.
- Migration issue: check Flyway history and SQL migration order.
- AI chat thread mismatch: ensure frontend sends `conversationId` only when continuing existing thread.

---

## Next reading

- `README.md` — product and architecture overview
- `AI_HISTORY_UI_INTEGRATION_GUIDE.md` — complete chat/history UI integration
- `PRODUCTION_DB_MIGRATION_GUIDE.md` — migration and deployment safety
- `API_GATEWAY_FRONTEND_MIGRATION.md` — gateway-first frontend integration
