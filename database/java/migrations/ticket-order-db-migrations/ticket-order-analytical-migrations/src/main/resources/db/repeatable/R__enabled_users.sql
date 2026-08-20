CREATE OR REPLACE VIEW enabled_users AS
SELECT
    id,
    email,
    role,
    created_at,
    updated_at
FROM users
WHERE enabled = TRUE;
