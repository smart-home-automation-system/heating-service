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

    //the row of heating_schedule it was read from (HAS-355); missing only for a schedule a test builds
    private Long id;

    @Builder.Default
    private ScheduleType type = ScheduleType.HEATING;

    @Singular
    private Set<DayOfWeek> days;

    private LocalTime startTime;

    private LocalTime endTime;

    private double temperature;

    //a disabled schedule is listed and never covers a moment (HAS-355)
    @Builder.Default
    private boolean enabled = true;

    //whether the schedule is on at that time of a day it judges as `day`, whatever the temperature of the
    //room. Both ends are exclusive. The day is the one HouseCalendar.scheduleDay gives the date - a day off
    //on a weekday is Sunday (HAS-358) - never the weekday of the clock alone. The one place for this rule:
    //the control loop decides by it (ScheduleService) and a reader is told by it (RoomMapper)
    public boolean covers(final LocalTime time, final DayOfWeek day) {
        return enabled
            && startTime.isBefore(time)
            && endTime.isAfter(time)
            && days.contains(day);
    }
}
