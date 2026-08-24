CREATE TABLE IF NOT EXISTS event_orders (
    event_order_id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    row_number INTEGER NOT NULL,
    place_number INTEGER NOT NULL,
    place_type VARCHAR(100) NOT NULL,
    reservation_date TIMESTAMP WITH TIME ZONE NOT NULL
);
