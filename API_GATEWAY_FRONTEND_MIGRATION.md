# Frontend Migration Guide — API Gateway Integration

All frontend API calls must now go through the **single API Gateway** at port `8080`
instead of talking to 4 separate backend services directly.

---

## Table of Contents

1. [What Changed and Why](#1-what-changed-and-why)
2. [New URL Mapping Reference](#2-new-url-mapping-reference)
3. [Step 1 — Update `.env` and `.env.example`](#3-step-1--update-env-and-envexample)
4. [Step 2 — Update `src/config/apiConfig.js`](#4-step-2--update-srcconfigapiconfigjs)
5. [Step 3 — Update `helper.js`](#5-step-3--update-helperjs)
6. [Step 4 — Fix Hardcoded URLs in Components](#6-step-4--fix-hardcoded-urls-in-components)
7. [Step 5 — Fix Missing `IDENTITY_SERVICE` Config Key](#7-step-5--fix-missing-identity_service-config-key)
8. [Complete Endpoint Migration Table](#8-complete-endpoint-migration-table)
9. [Production / Docker Configuration](#9-production--docker-configuration)
10. [Verification Checklist](#10-verification-checklist)

---

## 1. What Changed and Why

Previously, the frontend talked to 4 different backend ports:

```
http://localhost:2525  →  Main Backend      (Auth, Users, OTP)
http://localhost:2526  →  Contract Farming  (Agreements, Orders, Payments)
http://localhost:2527  →  Market Access     (Listings, Images)
http://localhost:2529  →  Generate Agreement (Contracts, Cold Storage)
```

A **Spring Cloud Gateway** is now running on port **8080**. It is the **only backend
port the frontend should ever talk to**. The gateway handles:

- Routing each request to the correct microservice using a **path prefix**
- Centralized CORS (so all services work from the same frontend origin)
- Circuit breaking — if a service is down, you get a clean `503 JSON` instead of a network error
- Global request/response logging
- Eureka-based load balancing — no hardcoded container IPs in production

**The only change** for the frontend is: prefix every URL with a service segment.

```
OLD: http://localhost:2525/auth/login
NEW: http://localhost:8080/main/auth/login
                          ^^^^^ new prefix
```

---

## 2. New URL Mapping Reference

| Service | Old base URL | Gateway prefix | New base URL |
|---------|-------------|----------------|--------------|
| Main Backend | `http://localhost:2525` | `/main` | `http://localhost:8080/main` |
| Contract Farming | `http://localhost:2526` | `/contract-farming` | `http://localhost:8080/contract-farming` |
| Market Access | `http://localhost:2527` | `/market` | `http://localhost:8080/market` |
| Generate Agreement | `http://localhost:2529` | `/agreement` | `http://localhost:8080/agreement` |

### Quick examples

```
# Auth
POST http://localhost:8080/main/auth/login
POST http://localhost:8080/main/auth/register
POST http://localhost:8080/main/auth/logout

# Users
GET  http://localhost:8080/main/users/{phone}
PUT  http://localhost:8080/main/users/update/{id}
GET  http://localhost:8080/main/users/profile-image/{id}

# Orders
POST http://localhost:8080/contract-farming/orders/create
GET  http://localhost:8080/contract-farming/orders/u/{userId}/paginated

# Payments
POST http://localhost:8080/contract-farming/api/payments/create-order
POST http://localhost:8080/contract-farming/api/payments/payment-callback

# Agreements
POST http://localhost:8080/contract-farming/agreements/save
GET  http://localhost:8080/contract-farming/agreements/get/{id}

# Upload/Download (IPFS)
POST http://localhost:8080/contract-farming/upload/{farmerAddress}/{buyerAddress}
GET  http://localhost:8080/contract-farming/download/{pdfHash}

# Listings
GET  http://localhost:8080/market/listings/all/active
GET  http://localhost:8080/market/listings/get/{id}
PUT  http://localhost:8080/market/listings/update/{id}

# Images
GET  http://localhost:8080/market/image/{id}

# Contracts (PDF generation)
POST http://localhost:8080/agreement/contracts/generate

# Cold Storage
GET  http://localhost:8080/agreement/coldStorage/bookings
POST http://localhost:8080/agreement/coldStorage/book
GET  http://localhost:8080/agreement/coldStorage/nearby/d/s
```

---

## 3. Step 1 — Update `.env` and `.env.example`

Replace the 4 separate service URLs with a single gateway URL.

**File:** `Frontend/frontend/.env`

```diff
 VITE_PORT=5000

-# Backend Microservices URLs
-VITE_MAIN_BACKEND_URL=http://localhost:2525
-VITE_CONTRACT_FARMING_URL=http://localhost:2526
-VITE_MARKET_ACCESS_URL=http://localhost:2527
-VITE_GENERATE_AGREEMENT_URL=http://localhost:2529
-VITE_EUREKA_SERVER_URL=http://localhost:8761
+# API Gateway — single entry point for all backend services
+VITE_API_GATEWAY_URL=http://localhost:8080

 # API Keys
 VITE_GEMINI_API_KEY=your_gemini_api_key_here
 VITE_OPENWEATHER_API_KEY=your_openweather_api_key_here
 VITE_MAPBOX_API_KEY=your_mapbox_public_token_here
 VITE_GOOGLE_MAPS_API_KEY=your_google_maps_api_key_here
```

Apply the same diff to `.env.example`.

> **Keep `VITE_EUREKA_SERVER_URL` only if you have a UI component that directly
> reads the Eureka registry (e.g. a service-status dashboard). If not, remove it.**

---

## 4. Step 2 — Update `src/config/apiConfig.js`

This is the **most important change**. Every component imports its base URL from
here, so fixing this file instantly fixes every component that uses `API_CONFIG`.

**File:** `Frontend/frontend/src/config/apiConfig.js`

```js
// Derive the gateway base URL from the environment variable.
// Falls back to localhost:8080 for local development.
const GATEWAY = import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080';

export const API_CONFIG = {
    // Each key now points to the gateway with the correct service prefix.
    // DO NOT append a trailing slash.
    MAIN_BACKEND:      `${GATEWAY}/main`,
    CONTRACT_FARMING:  `${GATEWAY}/contract-farming`,
    MARKET_ACCESS:     `${GATEWAY}/market`,
    GENERATE_AGREEMENT:`${GATEWAY}/agreement`,

    // IDENTITY_SERVICE was missing — it is an alias for MAIN_BACKEND (users endpoint).
    // Components like OrderItem.jsx and OrderList.jsx use this key.
    IDENTITY_SERVICE:  `${GATEWAY}/main`,
};

export const GEMINI_API_KEY     = import.meta.env.VITE_GEMINI_API_KEY;
export const OPENWEATHER_API_KEY = import.meta.env.VITE_OPENWEATHER_API_KEY || '3919f2231e07934b2048b0f97d3d5040';
```

> **Note:** `IDENTITY_SERVICE` was used in `OrderItem.jsx` and `OrderList.jsx` but
> was never defined in the original `apiConfig.js`. It is the same as `MAIN_BACKEND`
> because it calls `/users/unique/{id}`. This is fixed here.

---

## 5. Step 3 — Update `helper.js`

`helper.js` has a hardcoded `BASE_URL` that must be removed and replaced with
the `API_CONFIG` import.

**File:** `Frontend/frontend/helper.js`

```diff
 import axios from 'axios';
 import { jwtDecode } from 'jwt-decode';
+import { API_CONFIG } from './src/config/apiConfig.js';

-export const BASE_URL = "http://localhost:2525";
+export const BASE_URL = API_CONFIG.MAIN_BACKEND;   // http://localhost:8080/main

 // ... rest of the file stays the same ...
```

> If any component imports `BASE_URL` from `helper.js` and uses it for a
> non-Main-Backend call, fix that component's import to use the correct
> `API_CONFIG.*` key instead.

---

## 6. Step 4 — Fix Hardcoded URLs in Components

The following files contain hardcoded `localhost:{port}` URLs. Each one must be
updated to use `API_CONFIG`.

---

### `Component/CropContractAgreement/CropContractAgreement2.jsx`

This file has the most hardcoded URLs. Add the import at the top and replace:

```diff
+import { API_CONFIG } from '../../config/apiConfig.js';

-const response = await axios.get(`http://localhost:2525/users/${farmerContact}`, ...);
+const response = await axios.get(`${API_CONFIG.MAIN_BACKEND}/users/${farmerContact}`, ...);

-const res = await axios.post('http://localhost:2526/agreements/save', ...);
+const res = await axios.post(`${API_CONFIG.CONTRACT_FARMING}/agreements/save`, ...);

-const res2 = await axios.get(`http://localhost:2526/agreements/get/${agreementId}`, ...);
+const res2 = await axios.get(`${API_CONFIG.CONTRACT_FARMING}/agreements/get/${agreementId}`, ...);

-const uploadRes = await axios.post('http://localhost:2526/api/agreements/upload', ...);
+const uploadRes = await axios.post(`${API_CONFIG.CONTRACT_FARMING}/upload/${farmerAddress}/${buyerAddress}`, ...);

-const res3 = await axios.get(`http://localhost:2527/listings/get/${listingId}`, ...);
+const res3 = await axios.get(`${API_CONFIG.MARKET_ACCESS}/listings/get/${listingId}`, ...);

-await axios.put(`http://localhost:2527/listings/${listingId}/archived`, ...);
+await axios.put(`${API_CONFIG.MARKET_ACCESS}/listings/${listingId}/archived/${quantity}`, ...);

-const contractRes = await fetch('http://localhost:2529/contracts/generate', ...);
+const contractRes = await fetch(`${API_CONFIG.GENERATE_AGREEMENT}/contracts/generate`, ...);
```

---

### `Component/ColdStorage/NearbyColdStoragesMap.jsx`

```diff
-const response = await fetch('http://localhost:2529/coldStorage/nearby/d/s', ...);
+const response = await fetch(`${API_CONFIG.GENERATE_AGREEMENT}/coldStorage/nearby/d/s`, ...);
```

Add the import at the top if not already present:
```js
import { API_CONFIG } from '../../config/apiConfig.js';
```

---

### `Component/MyOrders/backup1.jsx`

This appears to be a backup/unused file. You can either:

- **Delete it** — it is not part of the active app.
- Or update its URLs using the same pattern as above (`API_CONFIG.CONTRACT_FARMING`,
  `API_CONFIG.MARKET_ACCESS`).

---

### `Component/MarketPlace/CropListingPage.jsx` and `Component/ListProductForm/ListingForm.jsx`

These components declare a **local `BASE_URL`** that shadows the one from `helper.js`.
Find the local variable and replace it:

```diff
-const BASE_URL = API_CONFIG.MARKET_ACCESS;   // if already using API_CONFIG — good
+// No change needed IF they're already doing: `${API_CONFIG.MARKET_ACCESS}/listings/...`
```

If they define a local `const BASE_URL = ...`, remove it and use `API_CONFIG.MARKET_ACCESS` directly.

---

### `Component/Payments/PaymentProcess.jsx` — redirect URL

The post-payment redirect is a frontend URL, not a backend URL. No change needed:

```js
window.location.href = "http://localhost:5000/my-orders"; // frontend redirect — OK
```

In production, this should also be an env variable:
```js
window.location.href = `${import.meta.env.VITE_APP_URL || 'http://localhost:5000'}/my-orders`;
```

---

## 7. Step 5 — Fix Missing `IDENTITY_SERVICE` Config Key

This is already handled in Step 2's `apiConfig.js` update by adding:

```js
IDENTITY_SERVICE: `${GATEWAY}/main`,
```

No individual component changes are needed — `API_CONFIG.IDENTITY_SERVICE` will
now resolve correctly wherever it is used (`OrderItem.jsx`, `OrderList.jsx`).

---

## 8. Complete Endpoint Migration Table

### Main Backend (`/main`)

| Old URL | New URL |
|---------|---------|
| `localhost:2525/auth/register` | `localhost:8080/main/auth/register` |
| `localhost:2525/auth/login/after/register` | `localhost:8080/main/auth/login/after/register` |
| `localhost:2525/auth/login` | `localhost:8080/main/auth/login` |
| `localhost:2525/auth/r/verify-otp/{phone}/{otp}` | `localhost:8080/main/auth/r/verify-otp/{phone}/{otp}` |
| `localhost:2525/auth/verify-otp/{phone}/{otp}` | `localhost:8080/main/auth/verify-otp/{phone}/{otp}` |
| `localhost:2525/auth/current-user` | `localhost:8080/main/auth/current-user` |
| `localhost:2525/auth/logout` | `localhost:8080/main/auth/logout` |
| `localhost:2525/auth/otp/send` | `localhost:8080/main/auth/otp/send` |
| `localhost:2525/auth/otp/verify` | `localhost:8080/main/auth/otp/verify` |
| `localhost:2525/auth/otp/initiate` | `localhost:8080/main/auth/otp/initiate` |
| `localhost:2525/users/{phone}` | `localhost:8080/main/users/{phone}` |
| `localhost:2525/users/unique/{id}` | `localhost:8080/main/users/unique/{id}` |
| `localhost:2525/users/update/{id}` | `localhost:8080/main/users/update/{id}` |
| `localhost:2525/users/profile-image/{id}` | `localhost:8080/main/users/profile-image/{id}` |
| `localhost:2525/users/signature-image/{id}` | `localhost:8080/main/users/signature-image/{id}` |

### Contract Farming (`/contract-farming`)

| Old URL | New URL |
|---------|---------|
| `localhost:2526/upload/{farmerAddr}/{buyerAddr}` | `localhost:8080/contract-farming/upload/{farmerAddr}/{buyerAddr}` |
| `localhost:2526/download/{pdfHash}` | `localhost:8080/contract-farming/download/{pdfHash}` |
| `localhost:2526/download/t/{txHash}` | `localhost:8080/contract-farming/download/t/{txHash}` |
| `localhost:2526/get/{orderId}` | `localhost:8080/contract-farming/get/{orderId}` |
| `localhost:2526/user/agreements/{userId}/paginated` | `localhost:8080/contract-farming/user/agreements/{userId}/paginated` |
| `localhost:2526/orders/create` | `localhost:8080/contract-farming/orders/create` |
| `localhost:2526/orders/{id}` | `localhost:8080/contract-farming/orders/{id}` |
| `localhost:2526/orders/paginated` | `localhost:8080/contract-farming/orders/paginated` |
| `localhost:2526/orders/u/{userId}/paginated` | `localhost:8080/contract-farming/orders/u/{userId}/paginated` |
| `localhost:2526/orders/buyer/{addr}/paginated` | `localhost:8080/contract-farming/orders/buyer/{addr}/paginated` |
| `localhost:2526/orders/farmer/{addr}/paginated` | `localhost:8080/contract-farming/orders/farmer/{addr}/paginated` |
| `localhost:2526/api/payments/create-order` | `localhost:8080/contract-farming/api/payments/create-order` |
| `localhost:2526/api/payments/payment-callback` | `localhost:8080/contract-farming/api/payments/payment-callback` |
| `localhost:2526/api/payments/confirm-delivery/{orderId}/{tracking}` | `localhost:8080/contract-farming/api/payments/confirm-delivery/{orderId}/{tracking}` |
| `localhost:2526/api/payments/verify-delivery/{orderId}` | `localhost:8080/contract-farming/api/payments/verify-delivery/{orderId}` |
| `localhost:2526/api/payments/request-return/{orderId}/{tracking}` | `localhost:8080/contract-farming/api/payments/request-return/{orderId}/{tracking}` |
| `localhost:2526/api/payments/confirm-return/{orderId}` | `localhost:8080/contract-farming/api/payments/confirm-return/{orderId}` |
| `localhost:2526/api/payments/reject-delivery/{orderId}` | `localhost:8080/contract-farming/api/payments/reject-delivery/{orderId}` |
| `localhost:2526/api/payments/buyer/{addr}` | `localhost:8080/contract-farming/api/payments/buyer/{addr}` |
| `localhost:2526/api/payments/farmer/{addr}` | `localhost:8080/contract-farming/api/payments/farmer/{addr}` |
| `localhost:2526/agreements/save` | `localhost:8080/contract-farming/agreements/save` |
| `localhost:2526/agreements/get/{id}` | `localhost:8080/contract-farming/agreements/get/{id}` |
| `localhost:2526/agreements/all` | `localhost:8080/contract-farming/agreements/all` |

### Market Access (`/market`)

| Old URL | New URL |
|---------|---------|
| `localhost:2527/listings/add` | `localhost:8080/market/listings/add` |
| `localhost:2527/listings/update/{id}` | `localhost:8080/market/listings/update/{id}` |
| `localhost:2527/listings/get/{id}` | `localhost:8080/market/listings/get/{id}` |
| `localhost:2527/listings/all` | `localhost:8080/market/listings/all` |
| `localhost:2527/listings/all/active` | `localhost:8080/market/listings/all/active` |
| `localhost:2527/listings/delete/{id}` | `localhost:8080/market/listings/delete/{id}` |
| `localhost:2527/listings/{id}/{status}/{qty}` | `localhost:8080/market/listings/{id}/{status}/{qty}` |
| `localhost:2527/listings/user/{contact}` | `localhost:8080/market/listings/user/{contact}` |
| `localhost:2527/listings/{id}/image` | `localhost:8080/market/listings/{id}/image` |
| `localhost:2527/image/{id}` | `localhost:8080/market/image/{id}` |

### Agreement Generator (`/agreement`)

| Old URL | New URL |
|---------|---------|
| `localhost:2529/contracts/generate` | `localhost:8080/agreement/contracts/generate` |
| `localhost:2529/coldStorage/book` | `localhost:8080/agreement/coldStorage/book` |
| `localhost:2529/coldStorage/approve/{bookingId}` | `localhost:8080/agreement/coldStorage/approve/{bookingId}` |
| `localhost:2529/coldStorage/bookings` | `localhost:8080/agreement/coldStorage/bookings` |
| `localhost:2529/coldStorage/nearby` | `localhost:8080/agreement/coldStorage/nearby` |
| `localhost:2529/coldStorage/nearby/d/s` | `localhost:8080/agreement/coldStorage/nearby/d/s` |
| `localhost:2529/coldStorage/{placeId}` | `localhost:8080/agreement/coldStorage/{placeId}` |

---

## 9. Production / Docker Configuration

When running via Docker Compose, the gateway is accessible at the host machine's
IP or domain name on port `8080`. The frontend `.env` for production should be:

```env
VITE_API_GATEWAY_URL=http://<your-server-ip-or-domain>:8080
```

Or if you place an Nginx reverse proxy in front:

```env
VITE_API_GATEWAY_URL=https://api.yourdomain.com
```

The `vite.config.js` reads `VITE_PORT` to set the dev server port. No change is
needed there. All backend calls go to `VITE_API_GATEWAY_URL`.

---

## 10. Verification Checklist

After making the changes above, verify the following in your browser's DevTools
**Network** tab:

### API Gateway is running
- [ ] `GET http://localhost:8080/actuator/health` returns `{"status":"UP"}`
- [ ] `GET http://localhost:8080/swagger-ui.html` opens the unified Swagger UI
- [ ] The Swagger UI dropdown shows all 4 services

### Auth flow works through the gateway
- [ ] Register: `POST http://localhost:8080/main/auth/register` → `200 OK`
- [ ] Login: `POST http://localhost:8080/main/auth/login` → `200 OK` + JWT cookie set
- [ ] Profile image: `GET http://localhost:8080/main/users/profile-image/{id}` → returns image

### Market Access works
- [ ] `GET http://localhost:8080/market/listings/all/active` → `200 OK` with listing array
- [ ] `GET http://localhost:8080/market/image/{id}` → returns image bytes

### Contract Farming works
- [ ] `GET http://localhost:8080/contract-farming/orders/u/{userId}/paginated` → `200 OK`

### No direct service calls remain
- [ ] Network tab shows **zero** requests to `localhost:2525`, `2526`, `2527`, or `2529`
- [ ] All backend requests target `localhost:8080`

### Circuit breaker fallback
- [ ] Stop one service (e.g., market-access). The app should receive a clean JSON:
  ```json
  {
    "status": 503,
    "error": "Service Unavailable",
    "service": "Market Access",
    "message": "The 'Market Access' service is temporarily unavailable..."
  }
  ```
  instead of a browser network error.

---

## Summary of All Files to Change

| File | Change |
|------|--------|
| `Frontend/frontend/.env` | Replace 4 service URLs with `VITE_API_GATEWAY_URL=http://localhost:8080` |
| `Frontend/frontend/.env.example` | Same as above |
| `Frontend/frontend/src/config/apiConfig.js` | Rewrite using single gateway URL + add `IDENTITY_SERVICE` key |
| `Frontend/frontend/helper.js` | Replace hardcoded `BASE_URL` with `API_CONFIG.MAIN_BACKEND` |
| `Component/CropContractAgreement/CropContractAgreement2.jsx` | Replace all 7 hardcoded `localhost:25xx` URLs |
| `Component/ColdStorage/NearbyColdStoragesMap.jsx` | Replace 1 hardcoded `localhost:2529` URL |
| `Component/MyOrders/backup1.jsx` | Delete file or update URLs (file is unused) |
| `Component/Payments/PaymentProcess.jsx` | Optional: make the `/my-orders` redirect URL configurable via env |
