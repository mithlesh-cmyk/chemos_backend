-- Manual deployment prerequisite. Do not run against production without approval.
-- No startup runner executes this script. Existing direct grants and user overrides
-- are unchanged; existing roles default to NONE (no implicit parent grants).
BEGIN;

ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS inheritance_mode varchar(16) NOT NULL DEFAULT 'NONE'
    CHECK (inheritance_mode IN ('NONE', 'ALL', 'SELECTED'));

CREATE TABLE IF NOT EXISTS role_inherited_permissions (
    role_id varchar(255) NOT NULL REFERENCES roles(id),
    permission_id varchar(255) NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

COMMIT;