CREATE SCHEMA IF NOT EXISTS ticket_features;
^^^ END OF STATEMENT ^^^

CREATE UNLOGGED TABLE IF NOT EXISTS ticket_features.t_event_order_projection (
    event_id UUID NOT NULL,
    row_number INTEGER NOT NULL,
    place_number INTEGER NOT NULL,
    projected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (event_id, row_number, place_number)
);
^^^ END OF STATEMENT ^^^

CREATE OR REPLACE FUNCTION ticket_features.project_event_order_insert()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO ticket_features.t_event_order_projection (
        event_id,
        row_number,
        place_number,
        projected_at
    )
    VALUES (
        NEW.event_id,
        NEW.row_number,
        NEW.place_number,
        CURRENT_TIMESTAMP
    )
    ON CONFLICT (event_id, row_number, place_number) DO NOTHING;

    RETURN NEW;
END;
$$;
^^^ END OF STATEMENT ^^^

CREATE OR REPLACE FUNCTION ticket_features.project_event_order_delete()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM ticket_features.t_event_order_projection
    WHERE event_id = OLD.event_id
      AND row_number = OLD.row_number
      AND place_number = OLD.place_number;

    RETURN OLD;
END;
$$;
^^^ END OF STATEMENT ^^^

DROP TRIGGER IF EXISTS t_event_order_projection_insert_trg
    ON ticket_transactional.t_event_order;
^^^ END OF STATEMENT ^^^

CREATE TRIGGER t_event_order_projection_insert_trg
AFTER INSERT ON ticket_transactional.t_event_order
FOR EACH ROW
EXECUTE FUNCTION ticket_features.project_event_order_insert();
^^^ END OF STATEMENT ^^^

DROP TRIGGER IF EXISTS t_event_order_projection_delete_trg
    ON ticket_transactional.t_event_order;
^^^ END OF STATEMENT ^^^

CREATE TRIGGER t_event_order_projection_delete_trg
AFTER DELETE ON ticket_transactional.t_event_order
FOR EACH ROW
EXECUTE FUNCTION ticket_features.project_event_order_delete();
^^^ END OF STATEMENT ^^^

GRANT USAGE ON SCHEMA ticket_features TO ticket_order;
^^^ END OF STATEMENT ^^^
GRANT SELECT, INSERT, DELETE ON ticket_features.t_event_order_projection TO ticket_order;
^^^ END OF STATEMENT ^^^
