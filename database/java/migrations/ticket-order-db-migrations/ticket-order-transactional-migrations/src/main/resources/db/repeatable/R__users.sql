CREATE OR REPLACE VIEW ticket_transactional.users AS
SELECT
    id,
    email,
    password_hash,
    role,
    enabled,
    created_at,
    updated_at
FROM ticket_transactional.t_users;
