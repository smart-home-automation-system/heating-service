package cloud.cholewa.heating.model;

import cloud.cholewa.home.model.RoomName;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
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
    @Builder.Default
    private List<HeaterActor> heaterActors = List.of();
    @Builder.Default
    private final List<OpeningSensor> openingSensors = List.of();
}
