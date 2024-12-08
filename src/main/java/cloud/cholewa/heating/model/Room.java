package cloud.cholewa.heating.model;

import cloud.cholewa.home.model.RoomName;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Room {
    private final RoomName name;
    private RoomMode mode;
    private boolean manualMode;
    private boolean heatingActive;
    private final Temperature temperature;
    private final Humidity humidity;
    private final List<HeaterActor> heaterActors;
    private final List<OpeningSensor> openingSensors;
    private final List<Schedule> schedules;

    public String getName() {
        return name.name();
    }
}
