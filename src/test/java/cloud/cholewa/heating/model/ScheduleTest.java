package cloud.cholewa.heating.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import static java.time.DayOfWeek.FRIDAY;
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
        assertThat(sut.covers(LocalDateTime.of(2026, 10, 8, 18, 32))).isTrue();
        assertThat(sut.covers(LocalDateTime.of(2026, 10, 9, 7, 0, 1))).isTrue();
    }

    @Test
    void should_not_cover_its_own_ends() {
        assertThat(sut.covers(LocalDateTime.of(2026, 10, 8, 7, 0))).isFalse();
        assertThat(sut.covers(LocalDateTime.of(2026, 10, 8, 23, 0))).isFalse();
        assertThat(sut.covers(LocalDateTime.of(2026, 10, 8, 22, 59, 59))).isTrue();
    }

    @Test
    void should_not_cover_a_moment_outside_its_hours() {
        assertThat(sut.covers(LocalDateTime.of(2026, 10, 8, 6, 59))).isFalse();
        assertThat(sut.covers(LocalDateTime.of(2026, 10, 8, 23, 30))).isFalse();
    }

    @Test
    void should_not_cover_another_day_of_the_week() {
        //a Saturday, at an hour of the schedule
        assertThat(sut.covers(LocalDateTime.of(2026, 10, 10, 18, 32))).isFalse();
    }
}
