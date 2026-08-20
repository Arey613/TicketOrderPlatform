CREATE TABLE IF NOT EXISTS t_event (
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

ALTER TABLE t_event
    ADD CONSTRAINT t_event_owner_id_fk
        FOREIGN KEY (owner_id) REFERENCES t_users(id);

ALTER TABLE t_event
    ADD CONSTRAINT t_event_status_check
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED'));
