# Quick Start Guide

Get the AgriConnect backend up and running in minutes.

---

## Prerequisites

**Option A — Docker (Recommended, easiest)**
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)

**Option B — Local Maven**
- [Java 21+](https://adoptium.net/)
- [MySQL 8.0+](https://dev.mysql.com/downloads/)
- [Maven 3.8+](https://maven.apache.org/download.cgi)

---

## Step 1: Clone the Repository

```bash
git clone <your-repo-url>
cd Backend
```

---

## Step 2: Configure Environment Variables

### Root `.env` (required for Docker Compose)

The root `.env` file already exists. Verify it has:

```properties
DOCKER_USERNAME=your_dockerhub_username
MYSQL_ROOT_PASSWORD=your_secure_mysql_password
TAG=latest

# Public URL of the API Gateway (used in Swagger UI "Try it out")
GATEWAY_URL=http://localhost:8080
```

### Per-service `.env` files

Copy the example file for each service and fill in your values:

```bash
# Windows (PowerShell)
Copy-Item Main-Backend\.env.example           Main-Backend\.env
Copy-Item Contract-Farming-App\.env.example   Contract-Farming-App\.env
Copy-Item Market-Access-App\.env.example      Market-Access-App\.env
Copy-Item Generate-Agreement-App\.env.example Generate-Agreement-App\.env

# macOS / Linux
cp Main-Backend/.env.example           Main-Backend/.env
cp Contract-Farming-App/.env.example   Contract-Farming-App/.env
cp Market-Access-App/.env.example      Market-Access-App/.env
cp Generate-Agreement-App/.env.example Generate-Agreement-App/.env
```

**Minimum values to set in each `.env`:**

| File | Required keys |
|------|--------------|
| `Main-Backend/.env` | `DB_PASSWORD`, `JWT_SECRET`, `TWILIO_*` |
| `Contract-Farming-App/.env` | `DB_PASSWORD`, `RAZORPAY_*` |
| `Market-Access-App/.env` | `DB_PASSWORD` |
| `Generate-Agreement-App/.env` | `DB_PASSWORD`, `MAIL_USERNAME`, `MAIL_PASS`, `GOOGLE_MAP_API_KEY` |

---

## Step 3: Start the Stack

### Option A — Docker Compose (Recommended)

```bash
docker compose up -d
```

This starts all 6 services in the correct order automatically:

```
MySQL → Eureka → [Main-Backend, Contract-Farming, Market-Access, Generate-Agreement] → Api-Gateway
```

Watch startup progress:
```bash
docker compose logs -f
```

Wait until you see all services show `Started ... in ... seconds` in the logs (~60–90 seconds total).

### Option B — Local Maven (6 terminals)

Open 6 terminals and run each command, **waiting for each step** before the next:

**Terminal 1 — Eureka (start first, wait for it)**
```bash
cd Eureka-Main-Server
mvn spring-boot:run
```
Wait until you see `Started EurekaMainServerApplication` then continue.

**Terminals 2–5 — Business services (start in any order)**
```bash
cd Main-Backend           && mvn spring-boot:run   # Terminal 2
cd Contract-Farming-App   && mvn spring-boot:run   # Terminal 3
cd Market-Access-App      && mvn spring-boot:run   # Terminal 4
cd Generate-Agreement-App && mvn spring-boot:run   # Terminal 5
```

**Terminal 6 — API Gateway (start last)**
```bash
cd Api-Gateway
mvn spring-boot:run
```

---

## Step 4: Verify Everything is Running

### 1. Eureka Dashboard — all services must be registered
Open: **http://localhost:8761**

You should see 5 services registered:
- `API-GATEWAY`
- `MAIN-BACKEND`
- `CONTRACT-FARMING-APP`
- `MARKET-ACCESS-APP`
- `GENERATE-AGREEMENT-APP`

### 2. Gateway health check
```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

### 3. Live gateway routes
```bash
curl http://localhost:8080/actuator/gateway/routes
# Shows all 4 active routes with their URIs and filters
```

### 4. Swagger UI
Open: **http://localhost:8080/swagger-ui.html**

Use the dropdown (top-right) to switch between services:
- Main Backend (Auth & Users)
- Contract Farming (Agreements, Orders, Payments)
- Market Access (Listings & Images)
- Agreement Generator (Contracts & Cold Storage)

---

## Step 5: Test the API

All requests go through **port 8080** (the API Gateway). The gateway prefixes route to each service:

| Service | Gateway prefix |
|---------|--------------|
| Main Backend | `http://localhost:8080/main` |
| Contract Farming | `http://localhost:8080/contract-farming` |
| Market Access | `http://localhost:8080/market` |
| Generate Agreement | `http://localhost:8080/agreement` |

### Register a new user

```bash
curl -X POST http://localhost:8080/main/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "John Doe",
    "phoneNumber": "9876543210",
    "address": "123 Farm Road, Village"
  }'
```

Expected: `{"message":"OTP sent successfully"}`

> For testing without Twilio, use test numbers: `8780850751` or `9924111980`

### Verify OTP and complete registration

```bash
curl -X POST http://localhost:8080/main/auth/r/verify-otp/9876543210/123456 \
  -H "Content-Type: application/json" \
  -d '{
    "username": "John Doe",
    "phoneNumber": "9876543210",
    "address": "123 Farm Road, Village"
  }'
```

### Login

```bash
curl -X POST http://localhost:8080/main/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "9876543210"}'
```

### Verify OTP and get JWT token

```bash
curl -X POST http://localhost:8080/main/auth/verify-otp/9876543210/123456 \
  -c cookies.txt
```

Expected response:
```json
{
  "jwtToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "role": "USER"
}
```

### Get all active marketplace listings

```bash
curl http://localhost:8080/market/listings/all/active
```

---

## Rebuilding a Single Service

When you change code in one service, rebuild only that service:

```bash
docker compose up --build --no-deps -d api-gateway
docker compose up --build --no-deps -d main-backend
docker compose up --build --no-deps -d contract-farming
docker compose up --build --no-deps -d market-access
docker compose up --build --no-deps -d generate-agreement
```

---

## Useful Commands

```bash
# View logs for a specific service
docker compose logs -f api-gateway
docker compose logs -f main-backend

# Restart a service without rebuilding
docker compose restart contract-farming

# Stop all services
docker compose down

# Stop and remove volumes (wipes database!)
docker compose down -v

# Check container status
docker compose ps
```

---

## Common Issues

### Gateway returns 503
The downstream service isn't running or hasn't registered with Eureka yet.
```bash
docker compose logs -f main-backend   # Check if it started
curl http://localhost:2525/actuator/health  # Direct health check
```

### Service not visible in Eureka dashboard
Wait 30–60 seconds — Eureka registration has a heartbeat delay. If still missing:
```bash
docker compose logs -f main-backend | grep -i eureka
```

### "Communications link failure" (database)
MySQL isn't ready yet. In Docker, services have health checks so this shouldn't happen. For local dev:
```bash
# Windows
net start MySQL

# Linux
sudo systemctl start mysql
```

### Swagger UI "Failed to fetch" on Try it out
Ensure `GATEWAY_URL=http://localhost:8080` is set in the root `.env`, then rebuild the affected service:
```bash
docker compose up --build --no-deps -d main-backend
```

### Port already in use
```bash
# Windows — find the process using port 8080
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/macOS
lsof -ti:8080 | xargs kill -9
```

### Build fails — clean and retry
```bash
cd Main-Backend
mvn clean install -DskipTests
```

---

## Next Steps

- **[README.md](README.md)** — Full architecture and configuration reference
- **[API_GATEWAY_FRONTEND_MIGRATION.md](API_GATEWAY_FRONTEND_MIGRATION.md)** — How to update the frontend to use the gateway
- **[PRODUCTION_CHECKLIST.md](PRODUCTION_CHECKLIST.md)** — Pre-deployment security and infrastructure checklist

---

**You're all set! Open http://localhost:8080/swagger-ui.html to explore the API.**
