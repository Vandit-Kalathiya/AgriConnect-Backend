# Quick Start Guide

Get the AgriConnect backend running locally with all services including the event-driven notification pipeline.

---

## Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| Java | 17+ | Required for all Spring Boot services |
| Maven | 3.8+ | Build tool |
| Docker Desktop | Latest | Required for Kafka, Schema Registry, PostgreSQL, and observability stack |
| PowerShell / Bash | Any | For running commands |

---

## Step 1: Clone the repository

```bash
git clone <repo-url>
cd Backend
```

---

## Step 2: Prepare environment files

Each service has its own `.env` file. Copy from the templates:

```powershell
# PowerShell
Copy-Item Api-Gateway\.env.example            Api-Gateway\.env
Copy-Item Market-Access-App\.env.example      Market-Access-App\.env
Copy-Item Contract-Farming-App\.env.example   Contract-Farming-App\.env
Copy-Item Generate-Agreement-App\.env.example Generate-Agreement-App\.env
Copy-Item Notification-Service\.env.example   Notification-Service\.env
Copy-Item Ws-Gateway\.env.example             Ws-Gateway\.env
```

```bash
# macOS / Linux
cp Api-Gateway/.env.example            Api-Gateway/.env
cp Market-Access-App/.env.example      Market-Access-App/.env
cp Contract-Farming-App/.env.example   Contract-Farming-App/.env
cp Generate-Agreement-App/.env.example Generate-Agreement-App/.env
cp Notification-Service/.env.example   Notification-Service/.env
cp Ws-Gateway/.env.example             Ws-Gateway/.env
```

Also review the root `.env` used by Docker Compose.

> **Important for Kafka connectivity:**  
> All services running on the host machine (not inside Docker) must use `localhost:29092` for `KAFKA_BOOTSTRAP_SERVERS` and `http://localhost:8081` for `SCHEMA_REGISTRY_URL`. The Docker-internal hostname `kafka:9092` is only reachable from within Docker containers.

---

## Step 3: Start the infrastructure (Docker)

The Docker Compose stack includes Kafka, Schema Registry, Kafka UI, Prometheus, and Grafana. **These must be started before any Spring Boot service.**

```bash
# Start all infrastructure containers
docker compose up -d

# Verify they are healthy
docker compose ps
```

**Infrastructure services exposed on localhost:**

| Service | Local URL | Purpose |
|---------|-----------|---------|
| Kafka broker | `localhost:29092` | Message broker (host access) |
| Schema Registry | `http://localhost:8081` | Avro schema management |
| Kafka UI | `http://localhost:8082` | Browse topics and messages |
| Prometheus | `http://localhost:9090` | Metrics scraping |
| Grafana | `http://localhost:3000` | Dashboards (admin/admin) |

Wait for all containers to show `healthy` or `running` status before proceeding.

---

## Step 4: Generate Avro sources

All services that produce or consume Kafka events use Avro-generated classes. Run this once (or after any schema change):

```powershell
# PowerShell — run in each producer/consumer service directory
cd Api-Gateway;           mvn generate-sources; cd ..
cd Market-Access-App;     mvn generate-sources; cd ..
cd Contract-Farming-App;  mvn generate-sources; cd ..
cd Generate-Agreement-App; mvn generate-sources; cd ..
cd Notification-Service;  mvn generate-sources; cd ..
```

---

## Step 5: Start Spring Boot services (startup order matters)

Start services in dependency order — Eureka must be first, then business services, then gateways last.

```bash
# 1 — Service registry (everything depends on this)
cd Eureka-Main-Server && mvn spring-boot:run

# 2 — Business services (order within this group does not matter)
cd Market-Access-App && mvn spring-boot:run
cd Contract-Farming-App && mvn spring-boot:run
cd Generate-Agreement-App && mvn spring-boot:run

# 3 — Notification pipeline
cd Notification-Service && mvn spring-boot:run

# 4 — Gateways (after all services are registered in Eureka)
cd Api-Gateway && mvn spring-boot:run
cd Ws-Gateway && mvn spring-boot:run
```

**Service ports:**

| Service | Default Port | Notes |
|---------|-------------|-------|
| Eureka-Main-Server | `8761` | Service registry UI |
| Market-Access-App | `2526` | Internal only |
| Contract-Farming-App | `2527` | Internal only |
| Generate-Agreement-App | `2529` | Internal only |
| Notification-Service | `2530` | Internal only |
| Api-Gateway | `8080` | **Public-facing HTTP/REST gateway** |
| Ws-Gateway | `8081` | **Public-facing WebSocket gateway** |

> Only ports `8080` and `8081` should be exposed to frontend clients. All other ports are internal.

---

## Step 6: Verify platform health

### Eureka dashboard
Open `http://localhost:8761` — all services should appear as `UP`.

### Gateway health
```
GET http://localhost:8080/actuator/health
```

### Gateway routes
```
GET http://localhost:8080/actuator/gateway/routes
```

Expected routes:
- `/main/**` → Market-Access-App
- `/contract/**` → Contract-Farming-App
- `/agreement/**` → Generate-Agreement-App
- `/notifications/**` → Notification-Service

### Swagger / API Docs
```
GET http://localhost:8080/swagger-ui.html
```

---

## Step 7: Smoke test core flows

### Authentication
```powershell
# Register
Invoke-RestMethod -Uri "http://localhost:8080/main/auth/register" -Method Post `
  -ContentType "application/json" `
  -Body '{"name":"Test User","email":"test@example.com","phone":"9876543210","password":"Test@123"}'

# Login
Invoke-RestMethod -Uri "http://localhost:8080/main/auth/login" -Method Post `
  -ContentType "application/json" `
  -Body '{"phone":"9876543210","password":"Test@123"}'
```

### Notification REST API
```powershell
# Unread count
Invoke-RestMethod -Uri "http://localhost:8080/notifications/api/notifications/unread-count?userId=<userId>" `
  -Headers @{ "Authorization" = "Bearer <jwt>" }

# List notifications
Invoke-RestMethod -Uri "http://localhost:8080/notifications/api/notifications?userId=<userId>" `
  -Headers @{ "Authorization" = "Bearer <jwt>" }
```

### Publish a test notification (Market-Access-App test endpoint)
```powershell
curl.exe -X POST "http://localhost:2526/notifications/test/publish" `
  -H "Content-Type: application/json" `
  -d "{\"userId\":\"<userId>\",\"message\":\"Test notification\"}"
```

---

## Step 8: Verify Kafka notification flow

1. Open Kafka UI at `http://localhost:8082`
2. Browse topic `agriconnect.notifications.auth` (or `market`, `contract`, `agreement`)
3. Messages should appear after performing any notifiable action (login, listing creation, etc.)
4. Check `Notification-Service` logs to confirm consumer received and processed the event
5. Check `agriconnect.notifications.dlq` — should be empty if everything is healthy

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
# Start all infrastructure
docker compose up -d

# Follow logs from all containers
docker compose logs -f

# Follow logs from a specific container
docker compose logs -f kafka
docker compose logs -f schema-registry

# Rebuild one container
docker compose up --build --no-deps -d kafka

# Stop infrastructure
docker compose down

# Stop and remove all data volumes (full reset)
docker compose down -v
```

---

## Troubleshooting checklist

| Problem | Likely cause | Fix |
|---------|-------------|-----|
| Service missing in Eureka | Wrong startup order, Eureka not ready | Restart missing service after Eureka is fully up |
| Gateway 5xx | Downstream service down or not registered | Check service health and Eureka registration |
| `UnknownHostException: kafka` | Service using `kafka:9092` instead of `localhost:29092` | Set `KAFKA_BOOTSTRAP_SERVERS=localhost:29092` in service `.env` |
| `Unknown magic byte!` from Kafka | Message not serialized as Avro | Always publish via service REST endpoints, not raw Kafka UI string producers |
| Migration issue | Flyway history mismatch | Check Flyway history table and SQL migration order |
| WebSocket connection refused | Ws-Gateway not started | Start `Ws-Gateway` service |
| Duplicate CORS headers on WebSocket | WebSocket going through Api-Gateway instead of Ws-Gateway | Use `ws://localhost:8081` (Ws-Gateway) for WebSocket connections |

---

## Next reading

- `README.md` — product and architecture overview
- `NOTIFICATION_UI_GUIDE.md` — WebSocket + REST notification integration for UI
- `NOTIFICATION_SCENARIOS.md` — all 28 notification trigger scenarios
- `AI_HISTORY_UI_INTEGRATION_GUIDE.md` — complete chat/history UI integration
- `PRODUCTION_CHECKLIST.md` — production deployment safety checklist
- `PRODUCTION_DB_MIGRATION_GUIDE.md` — migration and deployment safety
