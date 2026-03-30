# AgriConnect Backend — Microservices Architecture

A production-ready microservices backend for the AgriConnect agricultural platform, built with Spring Boot, Spring Cloud Gateway, and Eureka service discovery.

Key platform capabilities include centralized gateway routing, JWT-based authentication, production-grade logout token revocation, AI-powered market assistance in Market-Access-App (Groq-backed), and persistent AI conversation context for better chatbot continuity.

---

## Architecture Overview

```
                        ┌─────────────────────────────────┐
                        │         API Gateway :8080        │
                        │   Spring Cloud Gateway           │
                        │   • Routing (lb:// Eureka)       │
                        │   • Centralized CORS             │
                        │   • Circuit Breaker              │
                        │   • Global Logging               │
                        │   • Unified Swagger UI           │
                        └──────────┬──────────────────┬───┘
               ┌───────────────────┼──────────────┐   │
               ▼                   ▼              ▼   ▼
        /main/**          /contract-farming/**  /market/**  /agreement/**
               │                   │              │          │
    ┌──────────▼──┐    ┌───────────▼──┐  ┌───────▼──┐  ┌───▼────────────┐
    │ Main-Backend│    │Contract-Farm.│  │Market-Acc│  │Generate-Agreem.│
    │   :2525     │    │   :2526      │  │  :2527   │  │    :2529       │
    └─────────────┘    └──────────────┘  └──────────┘  └────────────────┘
           │                  │                │                │
           └──────────────────┴────────────────┴────────────────┘
                                       │
                          ┌────────────▼────────────┐
                          │  Eureka-Main-Server:8761 │
                          │     Service Registry     │
                          └─────────────────────────┘
```

### Services

| # | Service | Port | Responsibility |
|---|---------|------|---------------|
| 1 | **Api-Gateway** | **8080** | Single entry point — routing, CORS, circuit breaker, Swagger UI, JWT auth/logout |
| 2 | **Eureka-Main-Server** | 8761 | Service discovery & registry |
| 3 | **Main-Backend** | 2525 | Authentication, JWT, OTP, user management |
| 4 | **Contract-Farming-App** | 2526 | Farming contracts, orders, Razorpay payments, blockchain |
| 5 | **Market-Access-App** | 2527 | Product listings, images, marketplace, AI orchestration (chat/advisory/market/listing), AI persistence |
| 6 | **Generate-Agreement-App** | 2529 | PDF contract generation, cold storage, email |

> **All client traffic goes through port 8080 (the gateway). Never call service ports directly from the frontend.**

---

## Technology Stack

| Category | Technology |
|----------|-----------|
| Framework | Spring Boot 3.3–3.4 |
| Gateway | Spring Cloud Gateway (WebFlux/reactive) |
| Service Discovery | Spring Cloud Netflix Eureka |
| Resilience | Resilience4j Circuit Breaker |
| Security | Spring Security, JWT |
| Database | MySQL 8.0, JPA/Hibernate |
| API Docs | springdoc-openapi (Swagger UI) |
| Messaging | Twilio SMS |
| Payments | Razorpay |
| PDF | iTextPDF 8 |
| Blockchain | Web3j |
| Build | Maven |
| Java | 21–24 (per service) |
| Containers | Docker, Docker Compose |

---

## Prerequisites

**For Docker (recommended):**
- Docker Desktop

**For local development (manual run):**
- Java 21+
- MySQL 8.0+
- Maven 3.8+

---

## Quick Start

See **[QUICK_START.md](QUICK_START.md)** for step-by-step setup instructions.

---

## Environment Variables

### Root `.env` (Docker Compose infra variables)

```properties
DOCKER_USERNAME=your_dockerhub_username
TAG=latest
MYSQL_ROOT_PASSWORD=your_secure_password
GATEWAY_URL=http://localhost:8080        # Public gateway URL shown in Swagger UI
```

### Per-service `.env` files

Each service has its own `.env` file. Copy the `.env.example` for a template.

#### Main-Backend/.env
```properties
DB_URL=jdbc:mysql://localhost:3306/agri-connect?createDatabaseIfNotExist=true
DB_USERNAME=root
DB_PASSWORD=your_password
PORT=2525
JWT_SECRET=your_jwt_secret_min_256_bits
TWILIO_ACCOUNT_SID=your_twilio_sid
TWILIO_AUTH_TOKEN=your_twilio_token
TWILIO_PHONE_NUMBER=your_twilio_number
```

#### Contract-Farming-App/.env
```properties
DB_URL=jdbc:mysql://localhost:3306/agri-connect?createDatabaseIfNotExist=true
DB_USERNAME=root
DB_PASSWORD=your_password
PORT=2526
RAZORPAY_KEY_ID=your_razorpay_key
RAZORPAY_KEY_SECRET=your_razorpay_secret
CONTRACT_ADDRESS=0x0000000000000000000000000000000000000000
PRIVATE_KEY=0000000000000000000000000000000000000000000000000000000000000000
API_URL=http://localhost:8545
```

#### Generate-Agreement-App/.env
```properties
DB_URL=jdbc:mysql://localhost:3306/agri-connect?createDatabaseIfNotExist=true
DB_USERNAME=root
DB_PASSWORD=your_password
PORT=2529
MAIL_USERNAME=your_email@gmail.com
MAIL_PASS=your_app_password
GOOGLE_MAP_API_KEY=your_google_maps_key
```

#### Market-Access-App/.env
```properties
DB_URL=jdbc:mysql://localhost:3306/agri-connect?createDatabaseIfNotExist=true
DB_USERNAME=root
DB_PASSWORD=your_password
PORT=2527
```

---

## Running the Stack

### Option A — Docker Compose (Recommended)

```bash
# Start everything
docker compose up -d

# Rebuild a single service after code changes
docker compose up --build --no-deps -d api-gateway

# View logs
docker compose logs -f api-gateway

# Stop everything
docker compose down
```

**Startup order is handled automatically** via `depends_on` + `healthcheck` conditions.

### Option B — Local (Maven)

Start in this exact order:

```bash
# 1. Eureka first — all others depend on it
cd Eureka-Main-Server && mvn spring-boot:run

# 2. Business services (any order, separate terminals)
cd Main-Backend           && mvn spring-boot:run
cd Contract-Farming-App   && mvn spring-boot:run
cd Market-Access-App      && mvn spring-boot:run
cd Generate-Agreement-App && mvn spring-boot:run

# 3. Gateway last (after at least one service is registered in Eureka)
cd Api-Gateway            && mvn spring-boot:run
```

---

## Database Migrations (Production)

This repository is migration-ready with Flyway for services that received schema changes:

- `Market-Access-App`:
  - `db/migration/V20260329_01__ai_persistence_tables.sql`
- `Api-Gateway`:
  - `db/migration/V20260329_01__jwt_logout_hardening.sql`

Recommended production settings:

- `FLYWAY_ENABLED=true`
- `FLYWAY_BASELINE_ON_MIGRATE=true`
- `FLYWAY_BASELINE_VERSION=0`
- `JPA_DDL_AUTO=validate`

Deployment order:

1. Back up database
2. Deploy app build with Flyway enabled
3. Let Flyway apply versioned scripts
4. Verify `flyway_schema_history` and service health

Do not rely on `ddl-auto=update` in production.

Detailed runbook: **[PRODUCTION_DB_MIGRATION_GUIDE.md](PRODUCTION_DB_MIGRATION_GUIDE.md)**

---

## API Access

### All requests go through the gateway on port 8080

| Service | Gateway prefix | Example |
|---------|---------------|---------|
| Main Backend | `/main` | `POST http://localhost:8080/main/auth/login` |
| Contract Farming | `/contract-farming` | `GET http://localhost:8080/contract-farming/orders/all` |
| Market Access | `/market` | `GET http://localhost:8080/market/listings/all/active` |
| Generate Agreement | `/agreement` | `POST http://localhost:8080/agreement/contracts/generate` |

### Swagger UI (Unified)

Open: **http://localhost:8080/swagger-ui.html**

Use the dropdown in the top-right corner to switch between services. All "Try it out" calls go through the gateway automatically.

Individual service docs (for internal use):
- http://localhost:2525/swagger-ui.html — Main Backend
- http://localhost:2526/swagger-ui.html — Contract Farming
- http://localhost:2527/swagger-ui.html — Market Access
- http://localhost:2529/swagger-ui.html — Generate Agreement

---

## Security: Logout Lifecycle

`POST /main/auth/logout` uses a production-style, ordered logout pipeline:

1. **Blacklist token first** to immediately block replay
2. **Mark token/session expired** in persistence (targeted user/token only; no global session wipe)
3. **Clear auth cookie** (`jwt_token`) from the client

This flow is idempotent and supports header/cookie token extraction, helping ensure safe logout behavior across web and API clients.

---

## Market-Access AI Features

Market-Access-App now hosts backend AI orchestration endpoints (via gateway `/market/api/v1/ai/**`) with:

- Groq SDK provider integration (no frontend LLM key usage)
- Centralized safety/domain controls and fallback responses
- Conversation-aware chat responses using persisted DB history
- Persistence for chat messages plus non-chat AI interaction logs
- Configurable retention and scheduled batched cleanup for old AI logs

---

## Health & Monitoring

### Gateway (check all routes live)
```bash
# Gateway health
curl http://localhost:8080/actuator/health

# All registered routes
curl http://localhost:8080/actuator/gateway/routes
```

### Individual services
```bash
curl http://localhost:2525/actuator/health  # Main-Backend
curl http://localhost:2526/actuator/health  # Contract-Farming-App
curl http://localhost:2527/actuator/health  # Market-Access-App
curl http://localhost:2529/actuator/health  # Generate-Agreement-App
curl http://localhost:8761/actuator/health  # Eureka
```

### Eureka Dashboard
Open: http://localhost:8761 — shows all registered services and their instances.

### Prometheus Metrics
```bash
curl http://localhost:2525/actuator/prometheus
```

---

## Circuit Breaker

The gateway has Resilience4j circuit breakers on all 4 service routes. When a service is down or slow (> 10s), the circuit opens and returns a clean 503 instead of hanging the client:

```json
{
  "status": 503,
  "error": "Service Unavailable",
  "service": "Market Access",
  "message": "The 'Market Access' service is temporarily unavailable. Please try again in a moment.",
  "timestamp": "2026-02-22T10:30:00"
}
```

Circuit breaker settings (per service): sliding window 10, failure threshold 50%, open for 15s, auto-transition to half-open.

---

## CORS

CORS is handled **centrally by the gateway**. Individual services also have CORS configured for direct-access development. To avoid duplicate headers, the gateway uses `DedupeResponseHeader` to strip duplicate `Access-Control-Allow-Origin` values.

Allowed origins (configure via `cors.allowed.origins` env):
- `http://localhost:5000`
- `http://localhost:5173`
- `http://localhost:5174`

---

## Project Structure

```
Backend/
├── Api-Gateway/                   # Spring Cloud Gateway (NEW)
│   ├── src/main/java/.../
│   │   ├── ApiGatewayApplication.java
│   │   ├── config/OpenApiConfig.java
│   │   ├── controller/FallbackController.java
│   │   └── filter/LoggingFilter.java
│   ├── src/main/resources/application.yml
│   ├── Dockerfile
│   └── .dockerignore
├── Eureka-Main-Server/            # Service Discovery
├── Main-Backend/                  # Auth & Users
│   ├── src/main/java/.../
│   │   ├── config/                # UserConfig (Security), OpenApiConfig, etc.
│   │   ├── Controller/            # REST Controllers
│   │   ├── Service/               # Business logic
│   │   ├── Repository/            # Data access
│   │   ├── Entity/                # JPA entities
│   │   ├── DTO/                   # Data Transfer Objects
│   │   ├── exception/             # Global exception handler
│   │   └── jwt/                   # JWT filter & helper
│   └── .env
├── Contract-Farming-App/          # Contracts, Orders, Payments
├── Market-Access-App/             # Listings & Images
├── Generate-Agreement-App/        # PDF, Cold Storage, Email
├── docker-compose.yml
├── .env                           # Root Docker Compose vars
├── README.md                      # This file
├── QUICK_START.md                 # Get running in 10 minutes
├── PRODUCTION_CHECKLIST.md        # Pre-deployment checklist
└── API_GATEWAY_FRONTEND_MIGRATION.md  # Frontend URL migration guide
```

---

## Frontend Integration

When integrating a frontend, **all API calls must go through the gateway**. See **[API_GATEWAY_FRONTEND_MIGRATION.md](API_GATEWAY_FRONTEND_MIGRATION.md)** for the complete frontend migration guide, including:

- Updated `apiConfig.js` for the new single `GATEWAY_URL`
- Full endpoint mapping table (old ports → new gateway paths)
- List of hardcoded URLs to fix in components

---

## Adding a New Microservice

1. Create a Spring Boot project with Eureka Client dependency
2. Set `spring.application.name` — this is the Eureka service ID
3. Add `.env` and `.env.example` files
4. Configure `application.yml` with environment variables
5. Add `springdoc-openapi-starter-webmvc-ui` — use **`2.6.0`** for Spring Boot 3.3.x or **`2.7.0`** for Spring Boot 3.4.x
6. Create `OpenApiConfig.java` with `@Value("${GATEWAY_URL:http://localhost:8080}")` server URL
7. Add a new route to `Api-Gateway/src/main/resources/application.yml` under `spring.cloud.gateway.routes`
8. Add a fallback method in `Api-Gateway/.../controller/FallbackController.java`
9. Add the new service to `docker-compose.yml`
10. Update `README.md` and `QUICK_START.md`

### springdoc Version Matrix

| Spring Boot | Spring Framework | springdoc version |
|-------------|------------------|-------------------|
| 3.3.x | 6.1.x | `2.6.0` (webmvc-ui) |
| 3.4.x | 6.2.x | `2.7.0` (webmvc-ui) |
| Gateway 3.4.x | 6.2.x | `2.7.0` (webflux-ui) |

---

## Troubleshooting

### Service not visible in Eureka
1. Ensure Eureka server is running at `http://localhost:8761`
2. Verify `EUREKA_SERVER_URL` env variable
3. Wait 30–60 seconds for registration heartbeat

### Gateway returns 503 / circuit breaker open
1. Check if the downstream service is running: `curl http://localhost:25xx/actuator/health`
2. The circuit auto-closes after 15 seconds in half-open state
3. Check gateway logs: `docker compose logs -f api-gateway`

### Swagger UI "Failed to fetch" on Try it out
1. Ensure `GATEWAY_URL` is set correctly (default: `http://localhost:8080`)
2. The `servers[].url` in API docs must be the browser-accessible gateway URL

### Duplicate CORS header error
The gateway's `DedupeResponseHeader` filter handles this. If you see it, ensure `Api-Gateway/application.yml` has the `DedupeResponseHeader` default filter configured.

### `ClassNotFoundException: LiteWebJarsResourceResolver` (springdoc)
You are using springdoc `2.7.0+` on a **Spring Boot 3.3.x** service. Downgrade to `2.6.0` for that service.

### `NoSuchMethodError: ControllerAdviceBean.<init>(Object)` (springdoc)
You are using springdoc `2.6.0` on a **Spring Boot 3.4.x** service. Upgrade to `2.7.0` for that service.

### Bean definition override error (`filterChain`)
Two `@Configuration` classes in the same service both define a `filterChain` `@Bean`. Merge them into one or rename one bean using `@Bean("myFilterChain")`.

---

## Error Response Format

All services return standardized error responses:

```json
{
  "timestamp": "2026-01-25T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with phoneNumber: '9876543210'",
  "path": "/main/users/9876543210"
}
```

---

## Logging

Logs are written to `logs/` in each service directory:

| Service | Log file |
|---------|---------|
| Api-Gateway | `logs/api-gateway.log` |
| Main-Backend | `logs/main-backend.log` |
| Contract-Farming-App | `logs/contract-farming.log` |
| Market-Access-App | `logs/market-access.log` |
| Generate-Agreement-App | `logs/generate-agreement.log` |
| Eureka-Main-Server | `logs/eureka-server.log` |

---

## License

[Your License Here]

---

**Last Updated**: February 2026
**Version**: 2.0.0
