CREATE TABLE IF NOT EXISTS event_details (
    event_details_id UUID PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    description TEXT NOT NULL,
    number_of_places INTEGER NOT NULL,
    number_of_rows INTEGER NOT NULL,
    seats_per_row INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
