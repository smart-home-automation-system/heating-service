package cloud.cholewa.heating.model;

import cloud.cholewa.home.model.RoomName;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;

import java.util.List;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Room {

    private final RoomName name;

    private RoomMode mode;

    private boolean manualMode;

    private boolean isRoomHeatingEnabled;

    private final Temperature temperature;

    private final Humidity humidity;

    @Singular
    private List<HeaterActor> heaterActors;

    @Singular
    private final List<OpeningSensor> openingSensors;
}
