CREATE OR REPLACE VIEW ticket_analytical.enabled_users AS
SELECT
    id,
    email,
    role,
    created_at,
    updated_at
FROM ticket_transactional.t_users
WHERE enabled = TRUE;
