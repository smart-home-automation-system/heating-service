-- the last reading of a room is looked up per room, newest first
CREATE INDEX room_temperature_room_date_idx ON room_temperature (room, date DESC);

-- one row per sensor that is currently silent; removed when it reports again
CREATE TABLE temperature_sensor_alert
(
    id            INT PRIMARY KEY NOT NULL UNIQUE GENERATED ALWAYS AS IDENTITY,
    room          VARCHAR(50)     NOT NULL UNIQUE,
    stale_since   TIMESTAMP       NOT NULL,
    last_alert_at TIMESTAMP       NOT NULL
);
