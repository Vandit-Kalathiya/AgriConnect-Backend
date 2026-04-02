CREATE TABLE IF NOT EXISTS notifications
(
    id             UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    event_id       VARCHAR(64) NOT NULL UNIQUE,
    user_id        VARCHAR(64) NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    source_service VARCHAR(100) NOT NULL,
    template_id    VARCHAR(100) NOT NULL,
    channel        VARCHAR(20)  NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    error_message  TEXT,
    retry_count    INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id   ON notifications (user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status    ON notifications (status);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications (created_at);

CREATE TABLE IF NOT EXISTS notification_delivery_log
(
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    event_id   VARCHAR(64)  NOT NULL,
    channel    VARCHAR(20)  NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_delivery_log_event_channel UNIQUE (event_id, channel)
);

CREATE INDEX IF NOT EXISTS idx_delivery_log_event_id   ON notification_delivery_log (event_id);
CREATE INDEX IF NOT EXISTS idx_delivery_log_created_at ON notification_delivery_log (created_at);
