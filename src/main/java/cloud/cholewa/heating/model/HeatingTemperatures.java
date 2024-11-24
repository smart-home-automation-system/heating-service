package cloud.cholewa.heating.model;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class HeatingTemperatures {
    public static final double HOT_WATER_LOW_TEMPERATURE = 38;
    public static final double HOT_WATER_HIGH_TEMPERATURE = 42;
    public static final double FIREPLACE_START_TEMPERATURE = 45;
    public static final double FIREPLACE_ALERT_TEMPERATURE = 70;
    public static final double FIREPLACE_TEMPERATURE_VALID_TO_ENABLE_FURNACE = 40;
}
