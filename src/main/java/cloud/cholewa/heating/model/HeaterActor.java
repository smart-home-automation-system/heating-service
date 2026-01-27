package cloud.cholewa.heating.model;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    private boolean working;

    private LocalDateTime lastStatusUpdate;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private boolean inSchedule;

    private Double targetTemperature;
    
    @Singular
    private List<Schedule> schedules;
}
