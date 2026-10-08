package cloud.cholewa.heating.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
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

    //whether the schedule is on at that moment, whatever the temperature of the room. Both ends
    //are exclusive. The one place for this rule: the control loop decides by it (ScheduleService)
    //and a reader is told by it (RoomMapper)
    public boolean covers(final LocalDateTime moment) {
        return startTime.isBefore(moment.toLocalTime())
            && endTime.isAfter(moment.toLocalTime())
            && days.contains(moment.getDayOfWeek());
    }
}
