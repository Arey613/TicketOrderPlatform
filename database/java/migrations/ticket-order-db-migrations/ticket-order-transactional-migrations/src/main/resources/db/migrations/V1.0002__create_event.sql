CREATE TABLE IF NOT EXISTS ticket_transactional.t_event (
    event_id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES ticket_transactional.t_users(id),
    date TIMESTAMP WITH TIME ZONE NOT NULL,
    name VARCHAR(200) NOT NULL,
    place VARCHAR(200) NOT NULL,
    type VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT t_event_status_check CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED'))
);
