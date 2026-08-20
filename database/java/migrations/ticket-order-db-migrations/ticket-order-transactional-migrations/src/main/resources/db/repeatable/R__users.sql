CREATE OR REPLACE VIEW users AS
SELECT
    id,
    email,
    password_hash,
    role,
    enabled,
    created_at,
    updated_at
FROM t_users;
