-- Logout hardening schema updates for Api-Gateway.
-- Adds token lifecycle columns used by revocation-first logout flow.

ALTER TABLE jwt_token
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;

ALTER TABLE jwt_token
    ADD COLUMN IF NOT EXISTS revoked BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE jwt_token
    ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_jwt_token_token
    ON jwt_token (token);

CREATE INDEX IF NOT EXISTS idx_jwt_token_username
    ON jwt_token (username);

CREATE INDEX IF NOT EXISTS idx_jwt_token_revoked
    ON jwt_token (revoked);
