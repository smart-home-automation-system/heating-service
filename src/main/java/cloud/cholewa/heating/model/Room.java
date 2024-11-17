package cloud.cholewa.heating.model;

import cloud.cholewa.home.model.RoomName;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
public class Room {

    private final RoomName name;
    private boolean isHeatingActive;
    private TemperatureSensor temperatureSensor;
    private final List<OpeningSensor> openingSensors;
    private final List<HeaterActor> heaterActors;
}
