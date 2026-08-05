CREATE OR REPLACE VIEW ticket_analytical.users AS
SELECT
    id,
    email,
    role,
    enabled,
    created_at,
    updated_at
FROM ticket_transactional.t_users;
