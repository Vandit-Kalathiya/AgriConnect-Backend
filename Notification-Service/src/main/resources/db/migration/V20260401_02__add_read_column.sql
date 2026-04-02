ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS read BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_notifications_user_read
    ON notifications (user_id, read);
