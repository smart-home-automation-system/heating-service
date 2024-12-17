package cloud.cholewa.heating.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Schedule {
    private ScheduleType type;
    private Set<DayOfWeek> days;
    private LocalTime startTime;
    private LocalTime endTime;
    private double temperature;
}
