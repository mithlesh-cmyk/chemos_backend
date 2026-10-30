-- Manual script: enforce case-insensitive unique emails on users.
--
-- Production runs spring.jpa.hibernate.ddl-auto=validate, so Hibernate never creates this index.
-- Run by hand, once per database, AFTER the duplicate check below returns zero rows.
-- Not required by the email-service integration; apply it separately.

-- 1) Duplicate check (must return no rows):
SELECT lower(email) AS email, count(*) AS users
FROM users
WHERE email IS NOT NULL AND btrim(email) <> ''
GROUP BY lower(email)
HAVING count(*) > 1;

-- 2) Index (partial: users without an email are unaffected):
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email_lower
    ON users (lower(email))
    WHERE email IS NOT NULL;
