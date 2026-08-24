CREATE OR REPLACE VIEW event_orders AS
SELECT
    event_order_id,
    event_id,
    customer_id,
    row_number,
    place_number,
    place_type,
    reservation_date
FROM t_event_order;
