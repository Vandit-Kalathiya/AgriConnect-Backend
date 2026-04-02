# AgriConnect — Notification Scenarios Reference

> **Implementation Status:** All 28 notification scenarios are **fully implemented and wired** as of April 2026.  
> All four producer services have `NotificationEventPublisher` injected and calling `.publish()` at the correct business logic trigger points.

This document is the single source of truth for every notification event in the
system. It covers all four producer microservices, every business trigger, and
the exact code pattern to publish each event.

---

## Architecture Overview

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         Producer Microservices                            │
│                                                                           │
│  ┌─────────────┐  ┌──────────────────┐  ┌────────────────────┐            │
│  │ Api-Gateway │  │ Market-Access-App│  │Contract-Farming-App│            │
│  │  (auth)     │  │  (listings, AI)  │  │ (orders, payments) │            │
│  └──────┬──────┘  └────────┬─────────┘  └────────┬───────────┘            │
│         │                  │                      │                       │
│         │    ┌─────────────────────────┐          │                       │
│         │    │   Generate-Agreement-App│          │                       │
│         │    │    (PDF, cold storage)  │          │                       │
│         │    └────────────┬────────────┘          │                       │
│         │                 │                       │                       │
└─────────┼─────────────────┼───────────────────────┼───────────────────────┘
          │                 │                       │
          ▼                 ▼                       ▼
  agriconnect.     agriconnect.          agriconnect.        agriconnect.
  notifications.   notifications.        notifications.      notifications.
  auth             market                contract            agreement
          │                 │                       │                 │
          └─────────────────┴───────────────────────┴─────────────────┘
                                        │
                               ┌────────▼────────┐
                               │ Notification-   │
                               │ Service         │
                               │ (consumer)      │
                               └────────┬────────┘
                                        │ dispatches
                    ┌───────────────────┼───────────────────┐
                    ▼                   ▼                   ▼
              Email (SMTP)         SMS (Twilio)       In-App / Push
                                                    (WebSocket / FCM)
```

**Kafka topics consumed by Notification-Service:**


| Topic                                 | Producer Service                     |
| ------------------------------------- | ------------------------------------ |
| `agriconnect.notifications.auth`      | Api-Gateway                          |
| `agriconnect.notifications.market`    | Market-Access-App                    |
| `agriconnect.notifications.contract`  | Contract-Farming-App                 |
| `agriconnect.notifications.agreement` | Generate-Agreement-App               |
| `agriconnect.notifications.dlq`       | Notification-Service (failed events) |


---

## Priority Reference


| Priority   | Use case                                  |
| ---------- | ----------------------------------------- |
| `LOW`      | Informational, no immediate action needed |
| `NORMAL`   | Standard business events                  |
| `HIGH`     | Action-required, time-sensitive           |
| `CRITICAL` | Financial transactions, security events   |


---

## 1. Api-Gateway — Auth Events

**Topic:** `agriconnect.notifications.auth`  
**Source service label:** `auth-service`  
**Publisher location:** `AuthService`, `RegistrationVerificationService`, `PasswordResetService`


| #   | Trigger point                                                                    | Event Type                    | Template ID                   | Recipients | Channels       | Priority |
| --- | -------------------------------------------------------------------------------- | ----------------------------- | ----------------------------- | ---------- | -------------- | -------- |
| 1   | `RegistrationVerificationService.verifyAndRegister()` — OTP verified, user saved | `AUTH_WELCOME`                | `auth.welcome`                | New user   | EMAIL + IN_APP | NORMAL   |
| 2   | `RegistrationVerificationService.initiateRegistration()` — OTP dispatched        | `AUTH_REGISTRATION_OTP`       | `auth.registration.otp`       | New user   | EMAIL          | HIGH     |
| 3   | `AuthService.login()` — successful login detected from unrecognised device/IP    | `AUTH_NEW_DEVICE_LOGIN`       | `auth.new.device.login`       | User       | EMAIL + IN_APP | HIGH     |
| 4   | `PasswordResetService.initiateForgotPassword()` — reset OTP dispatched           | `AUTH_FORGOT_PASSWORD_OTP`    | `auth.forgot.password.otp`    | User       | EMAIL          | HIGH     |
| 5   | `PasswordResetService.resetPassword()` — password changed successfully           | `AUTH_PASSWORD_RESET_SUCCESS` | `auth.password.reset.success` | User       | EMAIL + IN_APP | HIGH     |
| 6   | `UserService.updateUser()` — profile fields or signature updated                 | `AUTH_PROFILE_UPDATED`        | `auth.profile.updated`        | User       | IN_APP         | LOW      |


### Payload fields per event

**AUTH_WELCOME**

```
userName      → user's full name
userPhone     → registered phone number
registeredAt  → ISO timestamp
```

**AUTH_REGISTRATION_OTP / AUTH_FORGOT_PASSWORD_OTP**

```
otp           → 6-digit code
expiresInMin  → "10"
userName      → user's name or phone
```

**AUTH_NEW_DEVICE_LOGIN**

```
deviceInfo    → user-agent string
ipAddress     → request IP
loginAt       → ISO timestamp
```

**AUTH_PASSWORD_RESET_SUCCESS**

```
userName      → user's name
resetAt       → ISO timestamp
```

**AUTH_PROFILE_UPDATED**

```
updatedFields → comma-separated field names e.g. "name,signature"
updatedAt     → ISO timestamp
```

---

## 2. Market-Access-App — Listing & AI Events

**Topic:** `agriconnect.notifications.market`  
**Source service label:** `market-access`  
**Publisher location:** `ListingService`, `AiController` (service layer)  
`**NotificationEventPublisher` status: already wired**


| #   | Trigger point                                                       | Event Type                        | Template ID               | Recipients | Channels       | Priority |
| --- | ------------------------------------------------------------------- | --------------------------------- | ------------------------- | ---------- | -------------- | -------- |
| 7   | `ListingService.addListing()` — new listing persisted               | `MARKET_LISTING_CREATED`          | `market.listing.created`  | Farmer     | EMAIL + IN_APP | NORMAL   |
| 8   | `ListingService.updateListingStatus()` — status set to `ACTIVE`     | `MARKET_LISTING_ACTIVE`           | `market.listing.active`   | Farmer     | IN_APP         | NORMAL   |
| 9   | `ListingService.updateListingStatus()` — status set to `SOLD`       | `MARKET_LISTING_SOLD`             | `market.listing.sold`     | Farmer     | EMAIL + IN_APP | HIGH     |
| 10  | `ListingService.updateListingStatus()` — status set to `EXPIRED`    | `MARKET_LISTING_EXPIRED`          | `market.listing.expired`  | Farmer     | IN_APP + EMAIL | NORMAL   |
| 11  | `ListingService.deleteListing()` — listing removed                  | `MARKET_LISTING_DELETED`          | `market.listing.deleted`  | Farmer     | IN_APP         | LOW      |
| 12  | `ListingService.updateListingStatus()` — quantity partially reduced | `MARKET_LISTING_QUANTITY_UPDATED` | `market.listing.quantity` | Farmer     | IN_APP         | NORMAL   |
| 13  | `AiController.listingPriceSuggestion()` — AI price suggestion ready | `MARKET_AI_PRICE_SUGGESTION`      | `market.ai.price`         | Farmer     | IN_APP         | LOW      |
| 14  | `AiController.cropRecommendations()` — crop advisory ready          | `MARKET_AI_CROP_RECOMMENDATION`   | `market.ai.crop`          | Farmer     | IN_APP         | LOW      |
| 15  | `AiController.marketCropAnalysis()` — market analysis ready         | `MARKET_AI_MARKET_ANALYSIS`       | `market.ai.market`        | Farmer     | IN_APP         | LOW      |


### Payload fields per event

**MARKET_LISTING_CREATED / ACTIVE / EXPIRED / DELETED**

```
listingId   → UUID of the listing
cropName    → name of the crop
quantity    → quantity + unit e.g. "500 kg"
listingDate → ISO timestamp
```

**MARKET_LISTING_SOLD**

```
listingId   → UUID
cropName    → crop name
quantity    → quantity sold
totalAmount → sale value in INR
soldAt      → ISO timestamp
```

**MARKET_LISTING_QUANTITY_UPDATED**

```
listingId        → UUID
cropName         → crop name
remainingQty     → new remaining quantity
purchasedQty     → qty just purchased
```

**MARKET_AI_PRICE_SUGGESTION**

```
cropName         → crop name
suggestedPrice   → e.g. "₹2,500/quintal"
marketTrend      → "RISING" | "STABLE" | "FALLING"
```

**MARKET_AI_CROP_RECOMMENDATION / MARKET_AI_MARKET_ANALYSIS**

```
district         → farmer's district
season           → current season
topCrop          → top recommended crop
reasonSummary    → one-line reason
```

---

## 3. Contract-Farming-App — Order, Payment & Agreement Events

**Topic:** `agriconnect.notifications.contract`  
**Source service label:** `contract-farming`  
**Publisher location:** `OrderService`, `PaymentService`, `AgreementService`  
`**NotificationEventPublisher` status: needs Kafka producer wiring (see Section 6)**

> Events marked **"BOTH"** require two separate `publish()` calls — one with the farmer's userId/email and one with the buyer's userId/email.


| #   | Trigger point                                                                | Event Type                    | Template ID                   | Recipients              | Channels       | Priority |
| --- | ---------------------------------------------------------------------------- | ----------------------------- | ----------------------------- | ----------------------- | -------------- | -------- |
| 16  | `OrderService.createOrder()` — order persisted                               | `CONTRACT_ORDER_CREATED`      | `contract.order.created`      | **BOTH** farmer + buyer | EMAIL + IN_APP | HIGH     |
| 17  | `PaymentService.createOrder()` — Razorpay order created                      | `CONTRACT_PAYMENT_INITIATED`  | `contract.payment.initiated`  | Buyer                   | IN_APP         | NORMAL   |
| 18  | `PaymentController.paymentCallback()` — Razorpay signature verified          | `CONTRACT_PAYMENT_SUCCESS`    | `contract.payment.success`    | **BOTH** farmer + buyer | EMAIL + IN_APP | CRITICAL |
| 19  | `PaymentService.confirmDelivery()` — buyer submits tracking number           | `CONTRACT_DELIVERY_CONFIRMED` | `contract.delivery.confirmed` | **BOTH** farmer + buyer | EMAIL + IN_APP | HIGH     |
| 20  | `PaymentService.verifyAndReleasePayment()` — escrow released to farmer       | `CONTRACT_PAYMENT_RELEASED`   | `contract.payment.released`   | Farmer                  | EMAIL + IN_APP | CRITICAL |
| 21  | `PaymentService.rejectAndRefundPayment()` — delivery rejected, refund issued | `CONTRACT_DELIVERY_REJECTED`  | `contract.delivery.rejected`  | **BOTH** farmer + buyer | EMAIL + IN_APP | CRITICAL |
| 22  | `PaymentService.requestReturn()` — return initiated by buyer                 | `CONTRACT_RETURN_REQUESTED`   | `contract.return.requested`   | **BOTH** farmer + buyer | EMAIL + IN_APP | HIGH     |
| 23  | `PaymentService.confirmReturn()` — farmer confirms goods received back       | `CONTRACT_RETURN_CONFIRMED`   | `contract.return.confirmed`   | Buyer                   | EMAIL + IN_APP | HIGH     |
| 24  | `AgreementService.uploadAgreement()` — PDF hashed and stored on blockchain   | `CONTRACT_AGREEMENT_UPLOADED` | `contract.agreement.uploaded` | **BOTH** farmer + buyer | EMAIL + IN_APP | HIGH     |
| 25  | `AgreementDetailsService.saveAgreementDetails()` — signatures captured       | `CONTRACT_AGREEMENT_SIGNED`   | `contract.agreement.signed`   | **BOTH** farmer + buyer | EMAIL + IN_APP | HIGH     |


### Payload fields per event

**CONTRACT_ORDER_CREATED**

```
orderId      → UUID
cropName     → crop name
quantity     → quantity + unit
totalAmount  → order value in INR
farmerName   → farmer's display name
buyerName    → buyer's display name
createdAt    → ISO timestamp
```

**CONTRACT_PAYMENT_INITIATED**

```
orderId          → UUID
razorpayOrderId  → Razorpay order ID
amount           → amount in INR
```

**CONTRACT_PAYMENT_SUCCESS**

```
orderId          → UUID
razorpayPaymentId→ payment ID
amount           → amount in INR
cropName         → crop name
paidAt           → ISO timestamp
```

**CONTRACT_DELIVERY_CONFIRMED**

```
orderId          → UUID
trackingNumber   → courier tracking number
confirmedAt      → ISO timestamp
```

**CONTRACT_PAYMENT_RELEASED**

```
orderId          → UUID
amount           → released amount in INR
releasedAt       → ISO timestamp
```

**CONTRACT_DELIVERY_REJECTED / CONTRACT_RETURN_REQUESTED / CONTRACT_RETURN_CONFIRMED**

```
orderId          → UUID
returnTracking   → return tracking number (if applicable)
reason           → rejection/return reason
actionAt         → ISO timestamp
```

**CONTRACT_AGREEMENT_UPLOADED / CONTRACT_AGREEMENT_SIGNED**

```
orderId          → UUID
pdfHash          → IPFS / blockchain hash
transactionHash  → blockchain transaction hash
uploadedAt       → ISO timestamp
```

---

## 4. Generate-Agreement-App — PDF & Cold Storage Events

**Topic:** `agriconnect.notifications.agreement`  
**Source service label:** `generate-agreement`  
**Publisher location:** `PdfGenerationService`, `ColdStorageService`  
`**NotificationEventPublisher` status: needs Kafka producer wiring (see Section 6)**


| #   | Trigger point                                                     | Event Type                        | Template ID                       | Recipients     | Channels       | Priority |
| --- | ----------------------------------------------------------------- | --------------------------------- | --------------------------------- | -------------- | -------------- | -------- |
| 26  | `PdfGenerationService.generateContractPdf()` — PDF bytes ready    | `AGREEMENT_PDF_GENERATED`         | `agreement.pdf.generated`         | Farmer + Buyer | EMAIL + IN_APP | HIGH     |
| 27  | `ColdStorageService.bookColdStorage()` — booking request saved    | `AGREEMENT_COLD_STORAGE_BOOKED`   | `agreement.cold.storage.booked`   | Farmer         | EMAIL + IN_APP | NORMAL   |
| 28  | `ColdStorageService.approveBooking()` — operator approves booking | `AGREEMENT_COLD_STORAGE_APPROVED` | `agreement.cold.storage.approved` | Farmer         | EMAIL + IN_APP | HIGH     |


### Payload fields per event

**AGREEMENT_PDF_GENERATED**

```
contractTitle  → e.g. "Wheat Supply Agreement — Season 2026"
farmerName     → farmer's name
buyerName      → buyer's name
generatedAt    → ISO timestamp
downloadHint   → "Download from the Agreements section"
```

**AGREEMENT_COLD_STORAGE_BOOKED**

```
coldStorageName → facility name
cropName        → crop to be stored
quantity        → quantity + unit
durationDays    → storage duration in days
bookedAt        → ISO timestamp
```

**AGREEMENT_COLD_STORAGE_APPROVED**

```
coldStorageName → facility name
cropName        → crop
approvedAt      → ISO timestamp
startDate       → storage start date
```

---

## 5. Universal Publish Pattern

All producer services use the same `NotificationEventPublisher` component.

### Generic template

```java
// Inject in your service
private final NotificationEventPublisher notificationEventPublisher;

@Value("${notification.topics.<topic-key>}")
private String notificationTopic;

// Call inside the business method after the main operation succeeds
private void publishNotification(String eventType,
                                 String userId,
                                 String templateId,
                                 List<String> channels,
                                 Map<String, String> payload,
                                 Priority priority,
                                 String correlationId,
                                 String recipientEmail,
                                 String recipientPhone) {
    try {
        var event = notificationEventPublisher.buildEvent(
            eventType, userId, templateId, channels,
            payload, priority, correlationId,
            recipientEmail, recipientPhone
        );
        notificationEventPublisher.publish(notificationTopic, event);
    } catch (Exception ex) {
        // Never let notification failure break the main business flow
        log.warn("[NOTIFY] Failed to publish {} for userId={}: {}", eventType, userId, ex.getMessage());
    }
}
```

### Concrete examples

**ORDER_CREATED — two publishes (farmer + buyer)**

```java
// In OrderService.createOrder(), after order.save():

// --- notify farmer ---
notificationEventPublisher.publish(contractTopic,
    notificationEventPublisher.buildEvent(
        "CONTRACT_ORDER_CREATED",
        order.getFarmerId(),
        "contract.order.created",
        List.of("EMAIL", "IN_APP"),
        Map.of(
            "orderId",     order.getId(),
            "cropName",    order.getCropName(),
            "quantity",    order.getQuantity() + " " + order.getUnit(),
            "totalAmount", order.getTotalAmount().toString(),
            "buyerName",   order.getBuyerName(),
            "createdAt",   Instant.now().toString()
        ),
        Priority.HIGH,
        order.getId(),           // correlationId
        farmer.getEmail(),
        farmer.getPhone()
    )
);

// --- notify buyer ---
notificationEventPublisher.publish(contractTopic,
    notificationEventPublisher.buildEvent(
        "CONTRACT_ORDER_CREATED",
        order.getBuyerId(),
        "contract.order.created",
        List.of("EMAIL", "IN_APP"),
        Map.of(
            "orderId",     order.getId(),
            "cropName",    order.getCropName(),
            "quantity",    order.getQuantity() + " " + order.getUnit(),
            "totalAmount", order.getTotalAmount().toString(),
            "farmerName",  order.getFarmerName(),
            "createdAt",   Instant.now().toString()
        ),
        Priority.HIGH,
        order.getId(),
        buyer.getEmail(),
        buyer.getPhone()
    )
);
```

**LISTING_SOLD — single publish (farmer only)**

```java
// In ListingService.updateListingStatus(), when newStatus == "SOLD":
notificationEventPublisher.publish(marketTopic,
    notificationEventPublisher.buildEvent(
        "MARKET_LISTING_SOLD",
        listing.getFarmerId(),
        "market.listing.sold",
        List.of("EMAIL", "IN_APP"),
        Map.of(
            "listingId",   listing.getId(),
            "cropName",    listing.getCropName(),
            "quantity",    listing.getQuantity() + " kg",
            "totalAmount", listing.getPrice().toString(),
            "soldAt",      Instant.now().toString()
        ),
        Priority.HIGH,
        listing.getId(),
        farmer.getEmail(),
        farmer.getPhone()
    )
);
```

**COLD_STORAGE_APPROVED — single publish (farmer only)**

```java
// In ColdStorageService.approveBooking(), after status update:
notificationEventPublisher.publish(agreementTopic,
    notificationEventPublisher.buildEvent(
        "AGREEMENT_COLD_STORAGE_APPROVED",
        booking.getFarmerId(),
        "agreement.cold.storage.approved",
        List.of("EMAIL", "IN_APP"),
        Map.of(
            "coldStorageName", booking.getFacilityName(),
            "cropName",        booking.getCropName(),
            "approvedAt",      Instant.now().toString(),
            "startDate",       booking.getStartDate().toString()
        ),
        Priority.HIGH,
        booking.getId(),
        farmer.getEmail(),
        null
    )
);
```

---

## 6. Implementation Checklist

> **Status as of April 2026: All services fully wired. All 28 scenarios active.**

### Market-Access-App — DONE ✅

- `NotificationEventPublisher` component exists ✅
- Kafka producer dependencies in `pom.xml` ✅
- `KafkaProducerConfig` bean configured ✅
- Avro schema + `avro-maven-plugin` set up ✅
- `notification.topics.market` property in `application.yml` ✅
- Trigger calls injected in `ListingService` (scenarios 7–12) ✅

### Api-Gateway — DONE ✅

- Kafka + Avro dependencies present ✅
- `KafkaTemplate` bean configured ✅
- `NotificationEventPublisher` in `com.agriconnect.api.gateway.kafka` ✅
- `notification.topics.auth: agriconnect.notifications.auth` in `application.yml` ✅
- Publisher injected into `AuthService`, `RegistrationVerificationService`, `PasswordResetService`, `UserService` ✅
- Trigger calls for scenarios 1–6 all wired ✅
- Registration OTP sends welcome-themed email (separate `sendRegistrationOtpEmail` method) ✅

### Contract-Farming-App — DONE ✅

- Kafka + Avro dependencies in `pom.xml` ✅
- `KafkaProducerConfig` + `application.yml` Kafka producer block ✅
- Avro schema `.avsc` + `avro-maven-plugin` + `build-helper-maven-plugin` ✅
- `NotificationEventPublisher` in `com.agriconnect.contract.kafka` ✅
- `notification.topics.contract: agriconnect.notifications.contract` in `application.yml` ✅
- Publisher injected into `OrderService`, `PaymentService`, `PaymentController`, `AgreementService` ✅
- Trigger calls for scenarios 16–25 all wired ✅

### Generate-Agreement-App — DONE ✅

- Kafka + Avro dependencies in `pom.xml` ✅
- `KafkaProducerConfig` + `application.yml` Kafka producer block ✅
- Avro schema `.avsc` + `avro-maven-plugin` + `build-helper-maven-plugin` ✅
- `NotificationEventPublisher` in `com.agriconnect.agreement.kafka` ✅
- `notification.topics.agreement: agriconnect.notifications.agreement` in `application.yml` ✅
- Publisher injected into `ColdStorageService` ✅
- Trigger calls for scenarios 26–28 wired ✅

---

## 7. Safety Rules

1. **Never let notification failures break business logic.** Wrap every `publish()` call in a try-catch that only logs a warning on failure.
2. **Use the business operation ID as `correlationId`** (orderId, listingId, bookingId). This gives the Notification-Service idempotency — duplicate events with the same `correlationId` are discarded.
3. **Call `publish()` AFTER the main entity is saved** — not before. If the save fails, no notification goes out.
4. **Two-recipient events** (farmer + buyer) require two separate `publish()` calls with different `userId` / `recipientEmail` values.
5. **OTP payloads** (scenarios 2, 4) must never be logged. Ensure log statements in `NotificationEventPublisher` do not print the full `payload` map for auth events.

---

## 8. All Event Types — Quick Reference

```
AUTH_WELCOME
AUTH_REGISTRATION_OTP
AUTH_NEW_DEVICE_LOGIN
AUTH_FORGOT_PASSWORD_OTP
AUTH_PASSWORD_RESET_SUCCESS
AUTH_PROFILE_UPDATED

MARKET_LISTING_CREATED
MARKET_LISTING_ACTIVE
MARKET_LISTING_SOLD
MARKET_LISTING_EXPIRED
MARKET_LISTING_DELETED
MARKET_LISTING_QUANTITY_UPDATED
MARKET_AI_PRICE_SUGGESTION
MARKET_AI_CROP_RECOMMENDATION
MARKET_AI_MARKET_ANALYSIS

CONTRACT_ORDER_CREATED
CONTRACT_PAYMENT_INITIATED
CONTRACT_PAYMENT_SUCCESS
CONTRACT_DELIVERY_CONFIRMED
CONTRACT_PAYMENT_RELEASED
CONTRACT_DELIVERY_REJECTED
CONTRACT_RETURN_REQUESTED
CONTRACT_RETURN_CONFIRMED
CONTRACT_AGREEMENT_UPLOADED
CONTRACT_AGREEMENT_SIGNED

AGREEMENT_PDF_GENERATED
AGREEMENT_COLD_STORAGE_BOOKED
AGREEMENT_COLD_STORAGE_APPROVED
```

Total: **28 notification scenarios** across 4 services.