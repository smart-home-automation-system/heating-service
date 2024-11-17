package cloud.cholewa.heating.home.model;

import cloud.cholewa.heating.model.TemperatureSensor;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Data
public class RoomConfigurationResponse {

    private String name;
    private boolean isHeatingActive;
    private TemperatureSensor temperature;
    private LocalDateTime temperatureUpdateTime;
    private boolean isAnyOpeningOpened;
}
