package cloud.cholewa.heating.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.THURSDAY;
import static org.assertj.core.api.Assertions.assertThat;

//the rule the control loop decides by and a reader is told by
class ScheduleTest {

    private final Schedule sut = Schedule.builder()
        .days(Set.of(THURSDAY, FRIDAY))
        .startTime(LocalTime.of(7, 0))
        .endTime(LocalTime.of(23, 0))
        .build();

    @Test
    void should_cover_a_moment_between_its_ends_on_one_of_its_days() {
        //a Thursday and a Friday
        assertThat(covers(sut, LocalDateTime.of(2026, 10, 8, 18, 32))).isTrue();
        assertThat(covers(sut, LocalDateTime.of(2026, 10, 9, 7, 0, 1))).isTrue();
    }

    @Test
    void should_not_cover_its_own_ends() {
        assertThat(covers(sut, LocalDateTime.of(2026, 10, 8, 7, 0))).isFalse();
        assertThat(covers(sut, LocalDateTime.of(2026, 10, 8, 23, 0))).isFalse();
        assertThat(covers(sut, LocalDateTime.of(2026, 10, 8, 22, 59, 59))).isTrue();
    }

    @Test
    void should_not_cover_a_moment_outside_its_hours() {
        assertThat(covers(sut, LocalDateTime.of(2026, 10, 8, 6, 59))).isFalse();
        assertThat(covers(sut, LocalDateTime.of(2026, 10, 8, 23, 30))).isFalse();
    }

    //HAS-355: listed, never on - so the loop never heats to it and a reader never takes it for the target
    @Test
    void should_never_cover_a_moment_while_disabled() {
        final Schedule disabled = Schedule.builder()
            .days(Set.of(THURSDAY, FRIDAY))
            .startTime(LocalTime.of(7, 0))
            .endTime(LocalTime.of(23, 0))
            .enabled(false)
            .build();

        assertThat(sut.isEnabled()).isTrue();
        assertThat(covers(disabled, LocalDateTime.of(2026, 10, 8, 18, 32))).isFalse();
    }

    @Test
    void should_not_cover_another_day_of_the_week() {
        //a Saturday, at an hour of the schedule
        assertThat(covers(sut, LocalDateTime.of(2026, 10, 10, 18, 32))).isFalse();
    }

    //HAS-358: judged by the day it is given - a holiday on a Thursday is given as Sunday
    @Test
    void should_judge_by_the_day_it_is_given_not_by_the_date() {
        assertThat(sut.covers(LocalTime.of(18, 32), SATURDAY)).isFalse();
        assertThat(sut.covers(LocalTime.of(18, 32), FRIDAY)).isTrue();
    }

    //the weekday of the moment - what every date is unless the calendar says otherwise
    private static boolean covers(final Schedule schedule, final LocalDateTime moment) {
        return schedule.covers(moment.toLocalTime(), moment.getDayOfWeek());
    }
}
