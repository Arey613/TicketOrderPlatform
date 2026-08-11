CREATE TABLE IF NOT EXISTS ticket_transactional.t_event_details (
    event_details_id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    description TEXT NOT NULL,
    number_of_places INTEGER NOT NULL,
    number_of_rows INTEGER NOT NULL,
    seats_per_row INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE ticket_transactional.t_event_details
    ADD CONSTRAINT t_event_details_event_id_unique
        UNIQUE (event_id);

ALTER TABLE ticket_transactional.t_event_details
    ADD CONSTRAINT t_event_details_event_id_fk
        FOREIGN KEY (event_id) REFERENCES ticket_transactional.t_event(event_id);

ALTER TABLE ticket_transactional.t_event_details
    ADD CONSTRAINT t_event_details_number_of_places_check
        CHECK (number_of_places > 0);

ALTER TABLE ticket_transactional.t_event_details
    ADD CONSTRAINT t_event_details_number_of_rows_check
        CHECK (number_of_rows > 0);

ALTER TABLE ticket_transactional.t_event_details
    ADD CONSTRAINT t_event_details_seats_per_row_check
        CHECK (seats_per_row > 0);
