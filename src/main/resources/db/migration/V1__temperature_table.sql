CREATE TABLE hot_water_temperature (
    id INT PRIMARY KEY NOT NULL UNIQUE GENERATED ALWAYS AS IDENTITY,
    timestamp TIMESTAMP NOT NULL,
    water_temperature NUMERIC(5,2),
    circulation_temperature NUMERIC(5,2)
);