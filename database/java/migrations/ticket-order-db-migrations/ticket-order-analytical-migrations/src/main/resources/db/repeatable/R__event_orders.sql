CREATE OR REPLACE VIEW ticket_analytical.event_orders AS
SELECT
    event_order_id,
    event_id,
    customer_reference,
    row_number,
    place_number,
    place_type,
    reservation_date
FROM ticket_transactional.t_event_order;
