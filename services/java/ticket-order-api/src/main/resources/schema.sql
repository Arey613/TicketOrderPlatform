CREATE TABLE IF NOT EXISTS t_users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS t_event (
    event_id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES t_users(id),
    date TIMESTAMP WITH TIME ZONE NOT NULL,
    name VARCHAR(200) NOT NULL,
    place VARCHAR(200) NOT NULL,
    type VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT t_event_status_check CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED'))
);

CREATE TABLE IF NOT EXISTS t_event_details (
    event_details_id UUID PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE REFERENCES t_event(event_id),
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

CREATE TABLE IF NOT EXISTS t_event_order (
    event_order_id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES t_event(event_id),
    customer_reference UUID NULL,
    row_number INTEGER NOT NULL,
    place_number INTEGER NOT NULL,
    place_type VARCHAR(100) NOT NULL,
    reservation_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT t_event_order_row_number_check CHECK (row_number > 0),
    CONSTRAINT t_event_order_place_number_check CHECK (place_number > 0),
    CONSTRAINT t_event_order_place_type_check CHECK (length(trim(place_type)) > 0),
    CONSTRAINT t_event_order_event_position_unique UNIQUE (event_id, row_number, place_number)
);
