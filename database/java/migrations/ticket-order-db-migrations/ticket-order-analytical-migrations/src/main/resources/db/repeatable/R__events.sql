CREATE OR REPLACE VIEW ticket_analytical.events AS
SELECT
    event_id,
    owner_id,
    date,
    name,
    place,
    type,
    status,
    created_at,
    updated_at
FROM ticket_transactional.t_event;
