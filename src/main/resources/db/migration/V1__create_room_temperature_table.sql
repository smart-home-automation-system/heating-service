CREATE TABLE room_temperature
(
    id          INT PRIMARY KEY NOT NULL UNIQUE GENERATED ALWAYS AS IDENTITY,
    date        TIMESTAMP       NOT NULL,
    room        VARCHAR(50)     NOT NULL,
    temperature NUMERIC(5, 2)   NOT NULL
);
