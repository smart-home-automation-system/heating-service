package cloud.cholewa.heating.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Schedule {

    private ScheduleType type;

    @Singular
    private Set<DayOfWeek> days;

    private LocalTime startTime;

    private LocalTime endTime;

    private double temperature;
}
