CREATE TABLE IF NOT EXISTS ticket_transactional.t_event_order (
    event_order_id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES ticket_transactional.t_event(event_id),
    customer_reference UUID NULL,
    row_number INTEGER NOT NULL,
    place_number INTEGER NOT NULL,
    place_type VARCHAR(100) NOT NULL,
    reservation_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT t_event_order_row_number_check CHECK (row_number > 0),
    CONSTRAINT t_event_order_place_number_check CHECK (place_number > 0),
    CONSTRAINT t_event_order_place_type_check CHECK (length(btrim(place_type)) > 0),
    CONSTRAINT t_event_order_event_position_unique UNIQUE (event_id, row_number, place_number)
);
