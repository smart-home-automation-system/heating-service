package cloud.cholewa.heating.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Schedule {
    private ScheduleType type;
    private LocalTime startTime;
    private LocalTime endTime;
    private int temperature;
}
