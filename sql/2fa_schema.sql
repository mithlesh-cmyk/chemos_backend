-- Manual schema script for the 2FA feature.
--
-- Why this exists: local/dev run with spring.jpa.hibernate.ddl-auto=update, so Hibernate
-- creates these tables automatically. Production runs ddl-auto=validate (it never creates
-- or alters tables), so these two tables must be created by hand, once, before deploying
-- any build that includes the 2FA code to an environment using ddl-auto=validate.
--
-- Run once against the target database:
--   psql "$DB_URL" -U "$DB_USERNAME" -f sql/2fa_schema.sql
--
-- Safe to re-run: every statement is guarded with IF NOT EXISTS.

CREATE TABLE IF NOT EXISTS two_factor_credentials (
    id                 UUID PRIMARY KEY,
    user_id            UUID NOT NULL UNIQUE REFERENCES users(id),
    encrypted_secret   TEXT,
    enabled            BOOLEAN NOT NULL DEFAULT FALSE,
    enrolled_at        TIMESTAMP,
    last_verified_at   TIMESTAMP,
    failed_attempts    INTEGER NOT NULL DEFAULT 0,
    locked_until       TIMESTAMP,
    last_used_code     VARCHAR(255),
    last_code_used_at  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS backup_codes (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id),
    code_hash   VARCHAR(255) NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    used_at     TIMESTAMP,
    created_at  TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_backup_codes_user_id ON backup_codes(user_id);
