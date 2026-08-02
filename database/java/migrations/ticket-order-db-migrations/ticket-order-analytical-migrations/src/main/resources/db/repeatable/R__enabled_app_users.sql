CREATE OR REPLACE VIEW ticket_analytical.enabled_app_users AS
SELECT
    id,
    email,
    role,
    created_at,
    updated_at
FROM ticket_transactional.t_app_users
WHERE enabled = TRUE;
