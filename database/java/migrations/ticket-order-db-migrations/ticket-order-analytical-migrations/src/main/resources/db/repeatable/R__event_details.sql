CREATE OR REPLACE VIEW ticket_analytical.event_details AS
SELECT
    event_details_id,
    event_id,
    description,
    number_of_places,
    number_of_rows,
    seats_per_row,
    created_at,
    updated_at
FROM ticket_transactional.t_event_details;
