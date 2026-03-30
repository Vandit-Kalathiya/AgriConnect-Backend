CREATE TABLE IF NOT EXISTS ai_kisan_mitra_history (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(100) NOT NULL,
    user_phone VARCHAR(30) NOT NULL,
    language VARCHAR(10),
    user_message VARCHAR(4000) NOT NULL,
    assistant_response VARCHAR(4000) NOT NULL,
    source VARCHAR(20),
    safety_decision VARCHAR(40),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ai_kisan_history_user_created
    ON ai_kisan_mitra_history (user_phone, created_at);

CREATE INDEX IF NOT EXISTS idx_ai_kisan_history_conversation_created
    ON ai_kisan_mitra_history (conversation_id, created_at);

CREATE TABLE IF NOT EXISTS ai_crop_advisory_history (
    id VARCHAR(36) PRIMARY KEY,
    user_phone VARCHAR(30) NOT NULL,
    language VARCHAR(10) NOT NULL,
    district VARCHAR(120) NOT NULL,
    state VARCHAR(120) NOT NULL,
    soil_type VARCHAR(120),
    season VARCHAR(120),
    response_payload VARCHAR(4000) NOT NULL,
    source VARCHAR(20),
    safety_decision VARCHAR(40),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ai_crop_history_user_created
    ON ai_crop_advisory_history (user_phone, created_at);
