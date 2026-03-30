-- Manual rollback for V20260329_01__jwt_logout_hardening.sql
-- Run only after impact assessment in controlled windows.

DROP INDEX IF EXISTS idx_jwt_token_revoked;
DROP INDEX IF EXISTS idx_jwt_token_username;
DROP INDEX IF EXISTS idx_jwt_token_token;

ALTER TABLE jwt_token DROP COLUMN IF EXISTS revoked_at;
ALTER TABLE jwt_token DROP COLUMN IF EXISTS revoked;
ALTER TABLE jwt_token DROP COLUMN IF EXISTS expires_at;
