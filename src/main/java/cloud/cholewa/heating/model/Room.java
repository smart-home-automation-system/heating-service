package cloud.cholewa.heating.model;

import cloud.cholewa.home.model.RoomName;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
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

    private volatile boolean isRoomHeatingEnabled;
    //whether a control pass has got as far as the heaters of the room since the service started.
    //Until then the false above is only the value the state starts with (RoomMapper)
    @Setter(AccessLevel.NONE)
    private volatile boolean heatingDecided;

    public void setRoomHeatingEnabled(final boolean roomHeatingEnabled) {
        this.isRoomHeatingEnabled = roomHeatingEnabled;
        this.heatingDecided = true;
    }

    private final Temperature temperature;

    private final Humidity humidity;

    @Singular
    private List<HeaterActor> heaterActors;

    @Singular
    private final List<OpeningSensor> openingSensors;
}
