-- AI persistence schema for Market-Access-App
-- Safe for existing databases (uses IF NOT EXISTS semantics where possible).

CREATE TABLE IF NOT EXISTS ai_conversations (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(100) NOT NULL UNIQUE,
    user_phone VARCHAR(30),
    language VARCHAR(10),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS ai_messages (
    id VARCHAR(36) PRIMARY KEY,
    conversation_ref_id VARCHAR(36) NOT NULL,
    sequence_no BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    endpoint_type VARCHAR(50) NOT NULL,
    content VARCHAR(4000),
    source VARCHAR(20),
    safety_decision VARCHAR(40),
    request_payload VARCHAR(4000),
    response_payload VARCHAR(4000),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_ai_messages_conversation
        FOREIGN KEY (conversation_ref_id)
        REFERENCES ai_conversations (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ai_conversation_user_updated
    ON ai_conversations (user_phone, updated_at);

CREATE INDEX IF NOT EXISTS idx_ai_msg_conversation_sequence
    ON ai_messages (conversation_ref_id, sequence_no);

CREATE INDEX IF NOT EXISTS idx_ai_msg_conversation_created
    ON ai_messages (conversation_ref_id, created_at);
