# Swagger UI Troubleshooting Guide

## Issue Fixed
The Swagger UI dropdown was showing API Gateway endpoints for all services instead of showing each service's specific endpoints.

## Root Cause
1. **Service naming mismatch** between Eureka registration and Gateway LoadBalancer
2. **Missing explicit routes** for `/v3/api-docs` endpoints
3. **Circuit breaker fallback** was interfering with API docs requests

## Changes Made

### 1. GatewayRoutesConfig.java
- ✅ Updated LoadBalancer service IDs to match exact `spring.application.name`:
  - `CONTRACT-FARMING-APP` → `Contract-Farming-App`
  - `MARKET-ACCESS-APP` → `Market-Access-App`
  - `GENERATE-AGREEMENT-APP` → `Generate-Agreement-App`
  - `NOTIFICATION-SERVICE` → `Notification-Service`

- ✅ Added dedicated routes for Swagger API docs (without circuit breaker):
  - `/contract-farming/v3/api-docs/**` → Contract-Farming-App
  - `/market/v3/api-docs/**` → Market-Access-App
  - `/agreement/v3/api-docs/**` → Generate-Agreement-App
  - `/notifications/v3/api-docs/**` → Notification-Service

### 2. Notification-Service OpenApiConfig.java
- ✅ Created missing OpenAPI configuration for consistent documentation

## Verification Steps

### Step 1: Check All Services Are Running
Ensure all services are started in this order:

1. **Eureka Server** (port 8761)
   ```bash
   cd Eureka-Main-Server
   mvnw spring-boot:run
   ```

2. **All Microservices**:
   - Contract-Farming-App (port 2526)
   - Market-Access-App (port 2527)
   - Generate-Agreement-App (port 2529)
   - Notification-Service (port 2530)

3. **API Gateway** (port 8080)
   ```bash
   cd Api-Gateway
   mvnw spring-boot:run
   ```

### Step 2: Verify Eureka Registration
1. Open browser: `http://localhost:8761`
2. Check "Instances currently registered with Eureka"
3. Verify all services are listed:
   - CONTRACT-FARMING-APP
   - MARKET-ACCESS-APP
   - GENERATE-AGREEMENT-APP
   - NOTIFICATION-SERVICE
   - API-GATEWAY

### Step 3: Test Individual Service API Docs
Test each service's API docs endpoint directly:

```bash
# Contract Farming
curl http://localhost:8080/contract-farming/v3/api-docs

# Market Access
curl http://localhost:8080/market/v3/api-docs

# Agreement Generator
curl http://localhost:8080/agreement/v3/api-docs

# Notification Service
curl http://localhost:8080/notifications/v3/api-docs

# API Gateway (direct)
curl http://localhost:8080/v3/api-docs
```

**Expected**: Each should return different JSON with service-specific endpoints.

### Step 4: Test Swagger UI
1. Open: `http://localhost:8080/swagger-ui.html`
2. Use the dropdown at top-right to select different services
3. Verify each service shows its own endpoints:
   - **API Gateway**: auth-controller, fallback-controller
   - **Contract Farming**: agreement-controller, order-controller, payment-controller
   - **Market Access**: listing-controller, image-controller
   - **Agreement Generator**: contract-generator-controller, cold-storage-controller
   - **Notification Service**: notification-controller

## Common Issues & Solutions

### Issue: Services not showing in Eureka
**Solution**: 
- Check service logs for Eureka registration errors
- Verify `EUREKA_SERVER_URL` environment variable
- Ensure Eureka server is running before starting services

### Issue: 404 when accessing `/market/v3/api-docs`
**Solution**:
- Verify Market-Access-App is running and registered
- Check gateway logs for routing errors
- Ensure service name matches exactly: `Market-Access-App`

### Issue: Still showing API Gateway endpoints
**Solution**:
- Clear browser cache and hard refresh (Ctrl+Shift+R)
- Restart API Gateway after all services are running
- Check browser console for JavaScript errors

### Issue: Circuit breaker fallback triggered
**Solution**:
- API docs routes now bypass circuit breaker (Order -2)
- Check if downstream service is healthy
- Review Resilience4j circuit breaker status

## Service Name Reference

| Service | spring.application.name | Eureka Registration | Gateway Route |
|---------|------------------------|---------------------|---------------|
| API Gateway | Api-Gateway | API-GATEWAY | N/A |
| Contract Farming | Contract-Farming-App | CONTRACT-FARMING-APP | /contract-farming/** |
| Market Access | Market-Access-App | MARKET-ACCESS-APP | /market/** |
| Agreement Generator | Generate-Agreement-App | GENERATE-AGREEMENT-APP | /agreement/** |
| Notification Service | Notification-Service | NOTIFICATION-SERVICE | /notifications/** |
| Eureka Server | Eureka-Main-Server | N/A | N/A |

## Testing Checklist

- [ ] Eureka server is running and accessible
- [ ] All 4 microservices are registered in Eureka
- [ ] API Gateway is running and registered
- [ ] Each service's `/v3/api-docs` endpoint returns unique JSON
- [ ] Swagger UI dropdown shows all 5 services
- [ ] Selecting each service shows different endpoints
- [ ] No 404 errors in browser console
- [ ] No circuit breaker fallbacks in gateway logs

## Additional Notes

- The Java 23 build error is unrelated to Swagger - ensure Java 23 is installed
- Services must be fully started and registered before testing Swagger UI
- Allow 5-10 seconds after starting services for Eureka registration to complete
- Check gateway logs for LoadBalancer resolution: `Resolved service 'Market-Access-App' to instance`
