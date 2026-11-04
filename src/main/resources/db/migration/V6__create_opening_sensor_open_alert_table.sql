-- one row per window or door sensor told to the household as possibly stuck open (HAS-350): it read
-- open for heating.window.stuck-open-after without a new window value while it held the heating of a
-- room that would heat. open_since - since when it reads open; state_updated_at - when its gateway last
-- heard the window value it still holds (the end comes with a newer one); alerted_at - when it was told.
-- A restart repeats nothing; the row goes with a newer value, or without a message once the sensor is
-- switched off, gone from the registry or in a room this service does not heat
CREATE TABLE opening_sensor_open_alert
(
    id               INT PRIMARY KEY NOT NULL UNIQUE GENERATED ALWAYS AS IDENTITY,
    sensor_id        BIGINT          NOT NULL UNIQUE,
    open_since       TIMESTAMP       NOT NULL,
    state_updated_at TIMESTAMP       NOT NULL,
    alerted_at       TIMESTAMP       NOT NULL
);
