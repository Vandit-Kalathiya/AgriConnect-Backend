# AI Backend Handover (v1)

This repository now includes centralized AI endpoints in `Market-Access-App`:

- `POST /api/v1/ai/chat/respond`
- `POST /api/v1/ai/crop/recommendations`
- `POST /api/v1/ai/market/crop-analysis`
- `POST /api/v1/ai/market/recommendations`
- `POST /api/v1/ai/listing/shelf-life`
- `POST /api/v1/ai/listing/price-suggestion`
- `GET /api/v1/ai/chat/conversations`
- `GET /api/v1/ai/chat/conversations/{conversationId}/messages`
- `PATCH /api/v1/ai/chat/conversations/{conversationId}/title`
- `DELETE /api/v1/ai/chat/conversations/{conversationId}`
- `GET /api/v1/ai/chat/history`
- `GET /api/v1/ai/crop/history`
- `DELETE /api/v1/ai/chat/history`
- `DELETE /api/v1/ai/history/all`

When called through API Gateway these are available at:

- `POST /market/api/v1/ai/chat/respond`
- `POST /market/api/v1/ai/crop/recommendations`
- `POST /market/api/v1/ai/market/crop-analysis`
- `POST /market/api/v1/ai/market/recommendations`
- `POST /market/api/v1/ai/listing/shelf-life`
- `POST /market/api/v1/ai/listing/price-suggestion`

## Implemented Backend Responsibilities

- Central AI orchestration service: `AiOrchestrationService`
- Provider adapter: `GroqProviderAdapter` via Groq Java SDK (backend key via `AI_GROQ_API_KEY`)
- Prompt template registry: `PromptTemplateRegistry`
- Safety layer: `SafetyPolicyService`
  - Agriculture domain filtering
  - Prompt injection pattern blocking
  - Basic PII redaction for cache key/log hygiene
- Conversation state:
  - `conversationId` generation and history truncation (`ConversationStoreService`)
- Cache:
  - TTL-based response cache (`AiCacheService`)
  - API-compatible with future Redis migration
- Reliability:
  - Retry attempts (`ai.retries`)
  - Provider timeout (`ai.timeout-ms`)
  - Fallback responses with source marker
- Governance:
  - Public chatbot throttling (`AiChatRateLimitFilter`)
  - Correlation IDs (`CorrelationIdFilter`, `X-Correlation-Id`)
- Existing gateway identity propagation (`X-User-Phone`) is required for non-public AI endpoints
- Strict thread ownership:
  - continue chat only with valid `conversationId` owned by authenticated user
  - create new conversation only when `conversationId` is not provided

## Response Contract Notes

Every response includes:

- `schemaVersion`
- `safetyDecision`
- `source` (`LLM`, `CACHE`, `FALLBACK`)

Chat response includes:

- `text`
- `conversationId`

Conversation list includes:

- stable `conversationId`
- conversation `title` (custom title or generated fallback)
- `lastMessagePreview`

## Config

Added `application.yml` keys:

- `ai.enabled`
- `ai.schema-version`
- `ai.provider`
- `ai.model`
- `ai.api-key`
- `ai.base-url`
- `ai.timeout-ms`
- `ai.retries`
- `ai.chat-history-limit`
- `ai.cache-ttl-seconds`
- `ai.chat-public-per-minute`
- `ai.persistence.enabled`
- `ai.persistence.context-window`
- `ai.persistence.retention-days`
- `ai.persistence.cleanup-enabled`
- `ai.persistence.cleanup-batch-size`
- `ai.persistence.cleanup-max-batches`
- `ai.persistence.cleanup-cron`

## Frontend Parity Mapping

- `getChatResponse()` -> `/api/v1/ai/chat/respond`
- `getGeminiResponse()` -> internal backend provider adapter only
- `generateCropRecommendations()` -> `/api/v1/ai/crop/recommendations`
- `generateCropAnalysis()` -> `/api/v1/ai/market/crop-analysis`
- `generateRecommendations()` -> `/api/v1/ai/market/recommendations`
- `predictShelfLife()` -> `/api/v1/ai/listing/shelf-life`
- `fetchAiPrice()` -> `/api/v1/ai/listing/price-suggestion`
- `listConversations()` -> `/api/v1/ai/chat/conversations`
- `getConversationMessages()` -> `/api/v1/ai/chat/conversations/{conversationId}/messages`
- `renameConversation()` -> `/api/v1/ai/chat/conversations/{conversationId}/title`
- `deleteConversation()` -> `/api/v1/ai/chat/conversations/{conversationId}`
- `deleteAllAiHistory()` -> `/api/v1/ai/history/all`

## Remaining Production Hardening (next steps)

- Replace in-memory cache with Redis for multi-instance consistency
- Add strict AI output JSON schema parser for provider payloads
- Add per-user/day quota limits and cost tracking meters
- Add canary feature flag at endpoint/model level
