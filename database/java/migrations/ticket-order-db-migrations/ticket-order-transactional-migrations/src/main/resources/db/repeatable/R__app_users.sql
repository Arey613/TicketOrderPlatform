CREATE OR REPLACE VIEW ticket_transactional.app_users AS
SELECT
    id,
    email,
    role,
    enabled,
    created_at,
    updated_at
FROM ticket_transactional.t_app_users;
