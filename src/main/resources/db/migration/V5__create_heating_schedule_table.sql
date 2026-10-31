-- The heating schedules of the heaters (HAS-355): until 1.12.0 they were Spring beans in HomeConfig,
-- so a change of an hour was a release. Loaded into the heaters at the start; the control loop reads
-- them from memory, never from here.
--   room    - the name of the room as the readings store it (RoomName.getValue: 'living room')
--   heater  - the heater of the room: 'radiator' or 'floor' (HeaterType)
--   days    - the days it is on, by constant name ('MONDAY' ... 'SUNDAY')
--   start_time, end_time - local time of the house; both ends exclusive, never across midnight
--   enabled - a disabled schedule is listed and never covers a moment
CREATE TABLE heating_schedule
(
    id          BIGINT PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY,
    room        VARCHAR(32)        NOT NULL,
    heater      VARCHAR(16)        NOT NULL,
    days        TEXT[]             NOT NULL,
    start_time  TIME               NOT NULL,
    end_time    TIME               NOT NULL,
    temperature NUMERIC(3, 1)      NOT NULL,
    enabled     BOOLEAN            NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP          NOT NULL DEFAULT (now() AT TIME ZONE 'Europe/Warsaw'),
    updated_at  TIMESTAMP          NOT NULL DEFAULT (now() AT TIME ZONE 'Europe/Warsaw'),
    CONSTRAINT heating_schedule_heater CHECK (heater IN ('radiator', 'floor')),
    CONSTRAINT heating_schedule_days CHECK (
        cardinality(days) > 0
            AND days <@ ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']),
    CONSTRAINT heating_schedule_times CHECK (start_time < end_time)
);

-- the order of the rows is the order of the schedules of a heater - the control loop heats to the first
-- one the room is still colder than - so the schedules of HomeConfig of 1.12.0 are inserted verbatim, in
-- their order
INSERT INTO heating_schedule (room, heater, days, start_time, end_time, temperature)
VALUES ('office', 'radiator', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'], '07:00', '23:00', 20.5),

       ('tobi', 'radiator', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'], '05:00', '08:00', 20.5),
       ('tobi', 'radiator', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'], '08:00', '17:00', 19.5),
       ('tobi', 'radiator', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'], '17:00', '22:00', 21.0),
       ('tobi', 'radiator', ARRAY ['SATURDAY', 'SUNDAY'], '09:00', '22:00', 21.0),

       ('livia', 'radiator', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'], '05:00', '08:00', 21.0),
       ('livia', 'radiator', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'], '08:00', '17:00', 20.0),
       ('livia', 'radiator', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'], '17:00', '22:00', 21.0),
       ('livia', 'radiator', ARRAY ['SATURDAY', 'SUNDAY'], '09:00', '22:00', 21.0),

       ('bedroom', 'radiator', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'], '07:00', '08:00', 16.0),
       ('bedroom', 'radiator', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'], '08:00', '21:00', 16.0),

       ('wardrobe', 'floor', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'], '05:00', '20:00', 19.5),

       ('bathroom up', 'radiator', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'], '17:00', '18:00', 22.0),
       ('bathroom up', 'floor', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'], '05:00', '09:00', 22.5),
       ('bathroom up', 'floor', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'], '17:00', '20:00', 22.5),

       ('living room', 'radiator', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'], '06:00', '08:00', 21.0),
       ('living room', 'radiator', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'], '15:00', '22:00', 21.0),
       ('living room', 'radiator', ARRAY ['SATURDAY', 'SUNDAY'], '08:00', '22:00', 21.0),
       ('living room', 'floor', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'], '06:00', '08:00', 21.0),
       ('living room', 'floor', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'], '15:00', '22:00', 21.0),
       ('living room', 'floor', ARRAY ['SATURDAY', 'SUNDAY'], '06:00', '22:00', 21.0),

       ('cinema', 'radiator', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'], '05:00', '08:00', 21.0),
       ('cinema', 'radiator', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'], '08:00', '16:00', 20.0),
       ('cinema', 'radiator', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'], '16:00', '22:00', 21.0),
       ('cinema', 'radiator', ARRAY ['SATURDAY', 'SUNDAY'], '08:00', '22:00', 21.0),

       ('bathroom down', 'radiator', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'], '16:00', '17:00', 23.0),
       ('bathroom down', 'floor', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'], '06:00', '07:00', 22.0),
       ('bathroom down', 'floor', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'], '18:00', '20:00', 22.0),

       ('entrance', 'radiator', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'], '05:00', '21:00', 16.5),

       ('garage', 'radiator', ARRAY ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'], '05:00', '20:00', 13.0);
