# Production DB Migration Guide

This guide explains how AgriConnect backend database migrations work in production, how to deploy safely, and how to roll back if needed.

## Why This Is Required

Production should not rely on `hibernate ddl-auto=update`.  
Instead, schema changes are versioned and deterministic through Flyway migration scripts.

## Services Covered

- `Api-Gateway`
  - Migration: `Api-Gateway/src/main/resources/db/migration/V20260329_01__jwt_logout_hardening.sql`
  - Manual rollback: `Api-Gateway/src/main/resources/db/rollback/R20260329_01__jwt_logout_hardening_rollback.sql`
- `Market-Access-App`
  - Migration: `Market-Access-App/src/main/resources/db/migration/V20260329_01__ai_persistence_tables.sql`
  - Migration: `Market-Access-App/src/main/resources/db/migration/V20260330_01__ai_chat_and_crop_history_tables.sql`
  - Migration: `Market-Access-App/src/main/resources/db/migration/V20260330_02__ai_conversation_title.sql`
  - Manual rollback: `Market-Access-App/src/main/resources/db/rollback/R20260329_01__ai_persistence_tables_rollback.sql`

## Runtime Settings (Required)

Set these in each service `.env` for production:

```properties
JPA_DDL_AUTO=validate
FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=true
FLYWAY_BASELINE_VERSION=0
FLYWAY_AUTO_REPAIR_ON_VALIDATION_ERROR=true
```

## Migration Lifecycle

1. Service boots.
2. Flyway checks `flyway_schema_history`.
3. Pending `V*` scripts are applied in order.
4. App starts only after successful migration.
5. JPA validates schema (`ddl-auto=validate`).

If Flyway validation fails due checksum mismatch, auto-repair can recover automatically when:

- `FLYWAY_AUTO_REPAIR_ON_VALIDATION_ERROR=true`

The app attempts:

1. `migrate`
2. `repair` (only when validation exception is detected)
3. `migrate` again

## Safe Deployment Checklist

1. Backup database.
2. Confirm migration scripts are present in image/jar.
3. Set Flyway/JPA envs as above.
4. Deploy one service at a time:
   - `Market-Access-App` (AI tables)
   - `Api-Gateway` (logout token hardening columns)
5. Verify:
   - `flyway_schema_history` has new rows
   - app health endpoints are `UP`
   - critical flows pass (login/logout, AI chat)

## Docker Compose Rollout (Command Guide)

Use this exact order from repository root:

```bash
# 1) Build updated images
docker compose build market-access api-gateway

# 2) Start/refresh Market-Access-App first (applies AI persistence migration)
docker compose up -d --no-deps market-access

# 3) Watch logs for Flyway success
docker compose logs -f market-access

# 4) Start/refresh Api-Gateway second (applies logout hardening migration)
docker compose up -d --no-deps api-gateway

# 5) Watch logs for Flyway success
docker compose logs -f api-gateway
```

Expected Flyway log indicators:

- `Successfully applied ... migration`
- service starts normally after migrations

Quick health checks (use your environment's configured base URLs):

```bash
curl <MARKET_ACCESS_BASE_URL>/actuator/health
curl <GATEWAY_BASE_URL>/actuator/health
```

## Verification Queries (PostgreSQL)

```sql
-- Flyway history
select installed_rank, version, description, success
from flyway_schema_history
order by installed_rank desc;

-- Api-Gateway hardening columns
select column_name
from information_schema.columns
where table_name = 'jwt_token'
  and column_name in ('expires_at', 'revoked', 'revoked_at');

-- Market-Access AI persistence tables
select table_name
from information_schema.tables
where table_name in ('ai_conversations', 'ai_messages');
```

## Rollback Strategy

Prefer forward-fix whenever possible.  
If rollback is unavoidable:

1. Disable incoming traffic / enable maintenance window.
2. Restore DB backup, or run manual rollback SQL in strict reverse order.
3. Re-deploy previous app version.
4. Validate health and core flows.

Manual rollback scripts are provided but must be executed deliberately:

- `Api-Gateway/src/main/resources/db/rollback/R20260329_01__jwt_logout_hardening_rollback.sql`
- `Market-Access-App/src/main/resources/db/rollback/R20260329_01__ai_persistence_tables_rollback.sql`

### Docker Compose rollback (if required)

```bash
# 1) Put app in maintenance mode / stop ingress traffic
# 2) Restore DB backup (preferred) OR run rollback SQL scripts manually
# 3) Re-deploy previous image tag
docker compose pull market-access api-gateway
docker compose up -d --no-deps market-access api-gateway

# 4) Verify health
curl <MARKET_ACCESS_BASE_URL>/actuator/health
curl <GATEWAY_BASE_URL>/actuator/health
```

## How AI Persistence Works (Operational Summary)

- Frontend sends `conversationId` with chatbot requests.
- `Market-Access-App` stores/reuses conversation context in `ai_conversations` + `ai_messages`.
- Separate history tables are maintained for chat/advisory UX support:
  - `ai_kisan_mitra_history`
  - `ai_crop_advisory_history`
- Recent context window is loaded efficiently using indexed queries.
- Non-chat AI endpoint interactions are logged compactly for audit/analysis.
- Scheduled cleanup removes old rows in bounded batches using:
  - `AI_PERSISTENCE_CLEANUP_ENABLED`
  - `AI_PERSISTENCE_CLEANUP_BATCH_SIZE`
  - `AI_PERSISTENCE_CLEANUP_MAX_BATCHES`
  - `AI_PERSISTENCE_CLEANUP_CRON`
  - retention policy from `AI_PERSISTENCE_RETENTION_DAYS`

## Final Operational Notes

- Keep `JPA_DDL_AUTO=validate` in production for both services.
- Keep Flyway enabled in production and CI.
- Keep `FLYWAY_AUTO_REPAIR_ON_VALIDATION_ERROR=true` for self-healing restarts after benign checksum drift.
- Never commit real secrets in `.env` files to source control.
- Rotate any exposed credentials before go-live.
