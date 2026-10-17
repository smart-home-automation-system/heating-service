-- one row per window or door sensor that is silent right now (HAS-232); removed when it reports
-- again, or without a message when the registry no longer has it switched on
CREATE TABLE opening_sensor_alert
(
    id            INT PRIMARY KEY NOT NULL UNIQUE GENERATED ALWAYS AS IDENTITY,
    sensor_id     BIGINT          NOT NULL UNIQUE,
    silent_since  TIMESTAMP       NOT NULL,
    last_alert_at TIMESTAMP       NOT NULL
);
