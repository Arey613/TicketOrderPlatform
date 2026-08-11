CREATE TABLE IF NOT EXISTS ticket_transactional.t_event_details (
    event_details_id UUID PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE REFERENCES ticket_transactional.t_event(event_id),
    description TEXT NOT NULL,
    number_of_places INTEGER NOT NULL,
    number_of_rows INTEGER NOT NULL,
    seats_per_row INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT t_event_details_number_of_places_check CHECK (number_of_places > 0),
    CONSTRAINT t_event_details_number_of_rows_check CHECK (number_of_rows > 0),
    CONSTRAINT t_event_details_seats_per_row_check CHECK (seats_per_row > 0)
);
