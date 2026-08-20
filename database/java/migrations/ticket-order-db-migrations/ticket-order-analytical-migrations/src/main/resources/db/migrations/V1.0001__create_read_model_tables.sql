CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    role VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS events (
    event_id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    date TIMESTAMP WITH TIME ZONE NOT NULL,
    name VARCHAR(200) NOT NULL,
    place VARCHAR(200) NOT NULL,
    type VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

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

CREATE TABLE IF NOT EXISTS event_orders (
    event_order_id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    customer_reference UUID NULL,
    row_number INTEGER NOT NULL,
    place_number INTEGER NOT NULL,
    place_type VARCHAR(100) NOT NULL,
    reservation_date TIMESTAMP WITH TIME ZONE NOT NULL
);
