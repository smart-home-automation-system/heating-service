package cloud.cholewa.heating.model;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class HeatingTemperatures {
    public static final double HOT_WATER_LOW_TEMPERATURE = 38;
    public static final double HOT_WATER_HIGH_TEMPERATURE = 42;
    public static final double FIREPLACE_START_TEMPERATURE = 45;
    public static final double FIREPLACE_ALERT_TEMPERATURE = 70;
    public static final double FIREPLACE_TEMPERATURE_VALID_TO_ENABLE_FURNACE = 40;
    public static final double CIRCULATION_TEMPERATURE_MAX = 22;
    public static final double FIREPLACE_TEMPERATURE_ALLOW_ENABLE_HEATER = 40;
    public static final double FIREPLACE_TEMPERATURE_ALLOW_DISABLE_HEATER = 37;
    public static final double ROOM_MIN_TEMP_TURN_ON_HEATING_BY_FIREPLACE = 20.5;
}
