package cloud.cholewa.heating.calendar;

import cloud.cholewa.home.model.DayType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//HAS-358: a day off on Monday to Friday runs the Sunday schedules; the calendar asked once a day
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class HouseCalendarTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    //Wednesday 11 November 2026, Independence Day
    private static final LocalDate HOLIDAY = LocalDate.of(2026, 11, 11);

    @Mock
    private CalendarClient calendarClient;

    private final AtomicReference<Instant> now = new AtomicReference<>(at(HOLIDAY, 8));

    private final Clock clock = new Clock() {
        @Override
        public ZoneId getZone() {
            return ZONE;
        }

        @Override
        public Clock withZone(final ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    };

    @Test
    void should_run_a_holiday_on_a_wednesday_by_the_sunday_schedules() {
        when(calendarClient.typeOf(HOLIDAY)).thenReturn(Mono.just(DayType.DAY_OFF));
        final HouseCalendar sut = new HouseCalendar(calendarClient, clock);

        sut.refresh().as(StepVerifier::create).verifyComplete();

        assertThat(sut.scheduleDay(HOLIDAY)).isEqualTo(DayOfWeek.SUNDAY);
    }

    @Test
    void should_leave_a_workday_its_weekday() {
        when(calendarClient.typeOf(HOLIDAY)).thenReturn(Mono.just(DayType.WORKDAY));
        final HouseCalendar sut = new HouseCalendar(calendarClient, clock);

        sut.refresh().as(StepVerifier::create).verifyComplete();

        assertThat(sut.scheduleDay(HOLIDAY)).isEqualTo(DayOfWeek.WEDNESDAY);
    }

    //a holiday on a Saturday is a Saturday: its schedules are the weekend's already
    @Test
    void should_leave_a_holiday_on_a_saturday_a_saturday() {
        final LocalDate saturday = LocalDate.of(2026, 8, 15);
        now.set(at(saturday, 8));
        when(calendarClient.typeOf(saturday)).thenReturn(Mono.just(DayType.DAY_OFF));
        final HouseCalendar sut = new HouseCalendar(calendarClient, clock);

        sut.refresh().as(StepVerifier::create).verifyComplete();

        assertThat(sut.scheduleDay(saturday)).isEqualTo(DayOfWeek.SATURDAY);
    }

    @Test
    void should_take_a_date_never_asked_about_for_its_weekday() {
        final HouseCalendar sut = new HouseCalendar(calendarClient, clock);

        assertThat(sut.scheduleDay(HOLIDAY)).isEqualTo(DayOfWeek.WEDNESDAY);
    }

    @Test
    void should_ask_the_calendar_once_a_day() {
        when(calendarClient.typeOf(HOLIDAY)).thenReturn(Mono.just(DayType.DAY_OFF));
        final HouseCalendar sut = new HouseCalendar(calendarClient, clock);

        sut.refresh().as(StepVerifier::create).verifyComplete();
        now.set(at(HOLIDAY, 20));
        sut.refresh().as(StepVerifier::create).verifyComplete();

        verify(calendarClient, times(1)).typeOf(any());
    }

    //the first pass after midnight asks about the new day, without a restart
    @Test
    void should_ask_about_the_next_day_once_it_has_begun() {
        final LocalDate next = HOLIDAY.plusDays(1);
        when(calendarClient.typeOf(HOLIDAY)).thenReturn(Mono.just(DayType.DAY_OFF));
        when(calendarClient.typeOf(next)).thenReturn(Mono.just(DayType.WORKDAY));
        final HouseCalendar sut = new HouseCalendar(calendarClient, clock);

        sut.refresh().as(StepVerifier::create).verifyComplete();
        now.set(at(next, 0).plusSeconds(60));
        sut.refresh().as(StepVerifier::create).verifyComplete();

        assertThat(sut.scheduleDay(HOLIDAY)).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(sut.scheduleDay(next)).isEqualTo(DayOfWeek.THURSDAY);
    }

    //a database-service away for a moment: the weekday decides, with a WARN, and the calendar is asked again
    //after ten minutes - not at every pass, and not never
    @Test
    void should_fall_back_to_the_weekday_when_the_calendar_cannot_be_read_and_ask_again_later(final CapturedOutput output) {
        when(calendarClient.typeOf(HOLIDAY))
            .thenReturn(Mono.error(new CalendarCallException("database-service answered 503")))
            .thenReturn(Mono.just(DayType.DAY_OFF));
        final HouseCalendar sut = new HouseCalendar(calendarClient, clock);

        sut.refresh().as(StepVerifier::create).verifyComplete();
        assertThat(sut.scheduleDay(HOLIDAY)).isEqualTo(DayOfWeek.WEDNESDAY);
        assertThat(output.getOut().lines().filter(line -> line.contains("could not be read")))
            .singleElement()
            .satisfies(line -> assertThat(line).contains(" WARN ").contains("WEDNESDAY"));

        now.set(now.get().plus(HouseCalendar.RETRY_AFTER).minusSeconds(1));
        sut.refresh().as(StepVerifier::create).verifyComplete();
        verify(calendarClient, times(1)).typeOf(HOLIDAY);

        now.set(now.get().plusSeconds(1));
        sut.refresh().as(StepVerifier::create).verifyComplete();
        assertThat(sut.scheduleDay(HOLIDAY)).isEqualTo(DayOfWeek.SUNDAY);
    }

    @Test
    void should_never_fail_a_refresh() {
        when(calendarClient.typeOf(HOLIDAY)).thenReturn(Mono.error(new IllegalStateException("boom")));
        final HouseCalendar sut = new HouseCalendar(calendarClient, clock);

        sut.refresh().as(StepVerifier::create).verifyComplete();
        verify(calendarClient, never()).typeOf(HOLIDAY.plusDays(1));
    }

    private static Instant at(final LocalDate date, final int hour) {
        return date.atTime(hour, 0).atZone(ZONE).toInstant();
    }

}
