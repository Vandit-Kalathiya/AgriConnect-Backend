CREATE TABLE IF NOT EXISTS notification_outbox
(
    id              UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    event_id        VARCHAR(64)  NOT NULL UNIQUE,
    topic           VARCHAR(200) NOT NULL,
    partition_key   VARCHAR(64)  NOT NULL,
    payload         TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count     INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_attempt_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_status     ON notification_outbox (status);
CREATE INDEX IF NOT EXISTS idx_outbox_created_at ON notification_outbox (created_at);
