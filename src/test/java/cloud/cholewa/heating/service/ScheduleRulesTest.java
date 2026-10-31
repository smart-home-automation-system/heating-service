package cloud.cholewa.heating.service;

import cloud.cholewa.heating.db.model.HeatingScheduleEntity;
import cloud.cholewa.heating.infrastructure.error.HeatingErrorId;
import cloud.cholewa.heating.infrastructure.error.ScheduleException;
import cloud.cholewa.heating.model.ScheduleBody;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

//Every rule of a written schedule, one by one: the dashboard copies them, so each is pinned with the
//reason it answers
class ScheduleRulesTest {

    private static final List<HeatingScheduleEntity> NONE = List.of();

    @Test
    void should_read_a_valid_schedule_with_its_days_in_the_order_of_the_week() {
        final ScheduleRules.Valid valid = ScheduleRules.check(body(List.of("SUNDAY", "MONDAY", "SATURDAY"), "08:00", "22:00", "21"), NONE);

        assertThat(valid.days()).containsExactly(MONDAY, SATURDAY, SUNDAY);
        assertThat(valid.dayNames()).containsExactly("MONDAY", "SATURDAY", "SUNDAY");
        assertThat(valid.startTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(valid.endTime()).isEqualTo(LocalTime.of(22, 0));
        assertThat(valid.temperature()).isEqualByComparingTo("21.0").hasToString("21.0");
    }

    @Test
    void should_refuse_a_missing_body() {
        assertRefused(null, "a schedule is required");
    }

    @Test
    void should_refuse_a_schedule_without_days() {
        assertRefused(body(null, "08:00", "22:00", "21"), "days are required");
        assertRefused(body(List.of(), "08:00", "22:00", "21"), "days are required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"monday", "FUNDAY", "Mon", ""})
    void should_refuse_a_day_that_is_not_a_constant_name_of_the_week(final String day) {
        assertRefused(body(List.of("MONDAY", day), "08:00", "22:00", "21"), "day " + day + " is not a day of the week");
    }

    @Test
    void should_refuse_a_day_listed_twice() {
        assertRefused(body(List.of("MONDAY", "MONDAY"), "08:00", "22:00", "21"), "day MONDAY is listed twice");
    }

    @Test
    void should_refuse_a_missing_time() {
        assertRefused(body(List.of("MONDAY"), null, "22:00", "21"), "startTime is required");
        assertRefused(body(List.of("MONDAY"), "08:00", null, "21"), "endTime is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"8:00", "08:00:00", "24:00", "08:60", "0800", "noon"})
    void should_refuse_a_time_that_is_not_hh_mm(final String time) {
        assertRefused(body(List.of("MONDAY"), time, "22:00", "21"), "startTime must be HH:mm - " + time + " is not");
        assertRefused(body(List.of("MONDAY"), "07:00", time, "21"), "endTime must be HH:mm - " + time + " is not");
    }

    @Test
    void should_refuse_a_start_that_is_not_before_the_end() {
        assertRefused(body(List.of("MONDAY"), "22:00", "08:00", "21"), "startTime must be before endTime");
        assertRefused(body(List.of("MONDAY"), "08:00", "08:00", "21"), "startTime must be before endTime");
    }

    @Test
    void should_refuse_a_missing_temperature() {
        assertRefused(body(List.of("MONDAY"), "08:00", "22:00", null), "temperature is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.5", "30.5", "-1", "100"})
    void should_refuse_a_temperature_out_of_range(final String temperature) {
        assertRefused(body(List.of("MONDAY"), "08:00", "22:00", temperature),
            "temperature must be from 5 to 30 degrees - " + temperature + " is not");
    }

    @ParameterizedTest
    @ValueSource(strings = {"20.2", "20.25", "21.75", "5.1"})
    void should_refuse_a_temperature_off_the_half_degree(final String temperature) {
        assertRefused(body(List.of("MONDAY"), "08:00", "22:00", temperature),
            "temperature must be in steps of 0.5 degrees - " + temperature + " is not");
    }

    @ParameterizedTest
    @ValueSource(strings = {"5", "30", "20.5", "20.50", "13.0"})
    void should_take_a_temperature_on_the_half_degree_within_the_range(final String temperature) {
        assertThat(ScheduleRules.check(body(List.of("MONDAY"), "08:00", "22:00", temperature), NONE).temperature())
            .isEqualByComparingTo(temperature);
    }

    @Test
    void should_refuse_a_window_that_overlaps_another_of_the_heater_on_a_day_in_common() {
        final List<HeatingScheduleEntity> others = List.of(stored(4L, "08:00", "17:00", true, "MONDAY", "TUESDAY"));

        assertRefused(body(List.of("TUESDAY"), "16:00", "22:00", "21"), others,
            "the window overlaps schedule 4 (MONDAY,TUESDAY 08:00-17:00)");
        assertRefused(body(List.of("MONDAY"), "09:00", "10:00", "21"), others, "overlaps schedule 4");
        assertRefused(body(List.of("MONDAY"), "06:00", "23:00", "21"), others, "overlaps schedule 4");
    }

    @Test
    void should_take_windows_that_touch() {
        final List<HeatingScheduleEntity> others = List.of(
            stored(4L, "08:00", "17:00", true, "MONDAY"),
            stored(5L, "05:00", "08:00", true, "MONDAY")
        );

        assertThat(ScheduleRules.check(body(List.of("MONDAY"), "17:00", "22:00", "21"), others).startTime())
            .isEqualTo(LocalTime.of(17, 0));
    }

    @Test
    void should_take_an_overlapping_window_on_other_days() {
        final List<HeatingScheduleEntity> others = List.of(stored(4L, "08:00", "17:00", true, "MONDAY", "TUESDAY"));

        assertThat(ScheduleRules.check(body(List.of("SATURDAY", "SUNDAY"), "09:00", "22:00", "21"), others).days())
            .containsExactly(SATURDAY, SUNDAY);
    }

    //switching it on later must not make two schedules meet
    @Test
    void should_count_a_disabled_schedule_in_the_overlap() {
        assertRefused(body(List.of("MONDAY"), "09:00", "10:00", "21"),
            List.of(stored(4L, "08:00", "17:00", false, "MONDAY")), "overlaps schedule 4");
    }

    @Test
    void should_answer_invalid_schedule_with_a_status_of_the_caller() {
        assertThatThrownBy(() -> ScheduleRules.check(body(List.of(), "08:00", "22:00", "21"), NONE))
            .isInstanceOfSatisfying(ScheduleException.class, exception -> {
                assertThat(exception.getError()).isEqualTo(HeatingErrorId.INVALID_SCHEDULE);
                assertThat(exception.getHttpStatus().value()).isEqualTo(400);
            });
    }

    private static void assertRefused(final ScheduleBody body, final String reason) {
        assertRefused(body, NONE, reason);
    }

    private static void assertRefused(final ScheduleBody body, final List<HeatingScheduleEntity> others, final String reason) {
        assertThatThrownBy(() -> ScheduleRules.check(body, others))
            .isInstanceOf(ScheduleException.class)
            .hasMessageContaining(reason);
    }

    private static ScheduleBody body(final List<String> days, final String from, final String to, final String temperature) {
        return new ScheduleBody(days, from, to, temperature == null ? null : new BigDecimal(temperature));
    }

    static HeatingScheduleEntity stored(final Long id, final String from, final String to, final boolean enabled, final String... days) {
        return new HeatingScheduleEntity(id, "office", "radiator", Arrays.asList(days), LocalTime.parse(from), LocalTime.parse(to),
            new BigDecimal("20.0"), enabled, null, null);
    }
}
