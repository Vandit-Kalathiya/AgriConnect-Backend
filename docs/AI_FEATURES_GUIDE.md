# AI Features Guide

## Overview
AI-powered chat, crop advisory, and Kisan Mitra features with conversation history and context management.

---

## Features

### 1. AI Chat
- Real-time conversational AI
- Context-aware responses
- Conversation history
- Multi-turn dialogue support

### 2. Crop Advisory
- Personalized crop recommendations
- Weather-based insights
- Soil analysis guidance
- Pest management advice

### 3. Kisan Mitra
- Farmer assistance chatbot
- Agricultural best practices
- Market price information
- Government scheme guidance

---

## API Endpoints

### Chat
```http
POST /api/ai/chat
{
  "message": "How to grow tomatoes?",
  "conversationId": "optional-id"
}
```

### Crop Advisory
```http
POST /api/ai/crop-advisory
{
  "cropType": "tomato",
  "location": "Maharashtra",
  "season": "Kharif"
}
```

### History
```http
GET /api/ai/conversations?page=0&size=20
GET /api/ai/conversations/{id}/messages
DELETE /api/ai/conversations/{id}
```

---

## Database Schema

### Tables
- `ai_conversations` - Conversation metadata
- `ai_messages` - Individual messages
- `ai_crop_advisory_history` - Advisory records
- `ai_kisan_mitra_history` - Kisan Mitra interactions

### Indexes
```java
@Index(name = "idx_ai_conversation_user_updated", columnList = "user_phone,updated_at")
@Index(name = "idx_ai_msg_conversation_sequence", columnList = "conversation_ref_id,sequence_no")
```

---

## Configuration

### Feature Flags
```properties
AI_ENABLED=true
AI_CHAT_ENABLED=true
AI_ADVISORY_ENABLED=true
AI_KISAN_MITRA_ENABLED=true
```

### AI Provider
```properties
AI_PROVIDER=openai
AI_API_KEY=your-api-key
AI_MODEL=gpt-4
```

---

## UI Integration

### Chat Component
```typescript
// Fetch conversations
GET /api/ai/conversations

// Send message
POST /api/ai/chat
{ message, conversationId }

// Load history
GET /api/ai/conversations/{id}/messages
```

### Display Format
- Show conversation list with last message preview
- Real-time message streaming
- Markdown rendering for responses
- Auto-scroll to latest message

---

## Performance

### Caching
- Conversation summaries cached (1h TTL)
- Recent messages cached (30m TTL)
- User context cached (2h TTL)

### Optimization
- Pagination for conversation list
- Lazy loading for message history
- Indexed queries for fast retrieval

---

## Best Practices

**DO:**
- ✅ Validate user input
- ✅ Handle rate limiting
- ✅ Cache frequent queries
- ✅ Clean up old conversations

**DON'T:**
- ❌ Store sensitive data in messages
- ❌ Skip error handling
- ❌ Ignore token limits
- ❌ Forget to paginate

---

## Related Files
- `AiConversationEntity.java` - Conversation model
- `AiMessageEntity.java` - Message model
- `AiConversationRepository.java` - Data access
