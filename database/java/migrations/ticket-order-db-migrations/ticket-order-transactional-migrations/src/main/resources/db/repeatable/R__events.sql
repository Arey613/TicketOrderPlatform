CREATE OR REPLACE VIEW events AS
SELECT
    event_id,
    owner_id,
    date,
    name,
    place,
    type,
    status,
    image_url,
    video_url,
    created_at,
    updated_at
FROM t_event;
