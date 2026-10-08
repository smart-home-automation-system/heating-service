package cloud.cholewa.heating.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HeaterActor {

    private final HeaterType type;

    private volatile boolean working;

    private volatile LocalDateTime lastStatusUpdate;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private volatile boolean inSchedule;

    private volatile Double targetTemperature;
    //whether a control pass has decided inSchedule since the service started. Until then the false
    //above is only the value the state starts with, and a reader is not told it (RoomMapper)
    @Setter(AccessLevel.NONE)
    private volatile boolean scheduleDecided;

    public void setInSchedule(final boolean inSchedule) {
        this.inSchedule = inSchedule;
        this.scheduleDecided = true;
    }
    
    @Singular
    private List<Schedule> schedules;
}
