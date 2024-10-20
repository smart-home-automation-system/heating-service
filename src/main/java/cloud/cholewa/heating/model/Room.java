package cloud.cholewa.heating.model;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
public class Room {

    private final RoomNames name;
    private boolean isHeatingActive;
    private final TemperatureSensor temperatureSensor;
    private final List<OpeningSensor> openingSensors;
    private final List<HeaterActor> heaterActors;
}
