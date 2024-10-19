package cloud.cholewa.heating.model;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
public class TemperatureSensor {

    private LocalDateTime updateTime;
    private double temperature;
    private double humidity;
}
