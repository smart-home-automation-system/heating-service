package cloud.cholewa.heating.calendar;

import cloud.cholewa.home.model.DayType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The day of the week a heating schedule judges a date by (HAS-358): <b>a day off of the house on Monday to
 * Friday counts as Sunday</b> - a Polish holiday or a day off of the household runs the weekend schedules.
 * A workday stays its weekday, and a Saturday or a Sunday stays itself. Every schedule of the house lists
 * Saturday and Sunday together, so "Sunday" is the weekend schedule; a schedule for Sunday without Saturday
 * would be the one a holiday runs.
 * <p>
 * The type of today is asked of the calendar of database-service once a day ({@link #refresh}, from the
 * decision of every minute and at the start) and kept in memory, so the control loop and the reads judge a
 * schedule without a call. A calendar that cannot be read leaves the day to its weekday - a holiday then
 * heats as the weekday it is - with a WARN, and is asked again after {@link #RETRY_AFTER}: a
 * database-service away for a moment must not cost the whole day. A date never asked about is its weekday.
 * A new day is asked about in the first pass after midnight.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HouseCalendar {

    static final Duration RETRY_AFTER = Duration.ofMinutes(10);
    private static final Set<DayOfWeek> WEEKEND = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    private final CalendarClient calendarClient;
    private final Clock clock;

    //a few days around today - what the calendar said of each, or the weekday after a failed read
    private final Map<LocalDate, Known> known = new ConcurrentHashMap<>();

    private record Known(DayType type, boolean fromCalendar, Instant askedAt) {
    }

    /**
     * Asks the calendar about today unless it answered for today already, or a failed read is less than
     * {@link #RETRY_AFTER} old. Never fails.
     */
    public Mono<Void> refresh() {
        return Mono.fromSupplier(() -> LocalDate.now(clock))
            .filter(this::due)
            .flatMap(this::ask)
            .then();
    }

    /** The day of the week the schedules of a date are judged by. */
    public DayOfWeek scheduleDay(final LocalDate date) {
        return runsAs(date, Optional.ofNullable(known.get(date)).map(Known::type).orElse(null));
    }

    private boolean due(final LocalDate date) {
        return Optional.ofNullable(known.get(date))
            .map(day -> !day.fromCalendar() && !clock.instant().isBefore(day.askedAt().plus(RETRY_AFTER)))
            .orElse(true);
    }

    private Mono<Known> ask(final LocalDate date) {
        return calendarClient.typeOf(date)
            .map(type -> new Known(type, true, clock.instant()))
            .doOnNext(day -> log.info("Day {} is a {} by the calendar of the house, the heating schedules run as on {}",
                date, day.type().getValue(), runsAs(date, day.type())))
            .onErrorResume(throwable -> {
                final Known weekday = new Known(byWeekday(date), false, clock.instant());
                log.warn("The calendar of {} could not be read ({}), the heating schedules run as on {}",
                    date, throwable.toString(), date.getDayOfWeek());
                return Mono.just(weekday);
            })
            .doOnNext(day -> {
                known.keySet().removeIf(kept -> kept.isBefore(date.minusDays(2)) || kept.isAfter(date.plusDays(2)));
                known.put(date, day);
            });
    }

    //a type not known is no day off: the weekday decides
    private static DayOfWeek runsAs(final LocalDate date, final DayType type) {
        return type == DayType.DAY_OFF && !WEEKEND.contains(date.getDayOfWeek()) ? DayOfWeek.SUNDAY : date.getDayOfWeek();
    }

    private static DayType byWeekday(final LocalDate date) {
        return WEEKEND.contains(date.getDayOfWeek()) ? DayType.DAY_OFF : DayType.WORKDAY;
    }
}
