package cloud.cholewa.heating.service;

import cloud.cholewa.heating.db.model.HeatingScheduleEntity;
import cloud.cholewa.heating.infrastructure.error.ScheduleException;
import cloud.cholewa.heating.model.ScheduleBody;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Every rule of a written heating schedule (HAS-356), in one place - the dashboard copies them, so a rule
 * changed here is changed there too. The first broken rule is the answer, {@code INVALID_SCHEDULE} with the
 * reason in English:
 * <ul>
 *     <li>at least one day, each a day of the week by its constant name, none twice;</li>
 *     <li>{@code startTime} and {@code endTime} as {@code HH:mm}, the start before the end - a window never
 *     runs across midnight (both ends are exclusive, so 08:00-17:00 and 17:00-22:00 touch, never meet);</li>
 *     <li>a temperature from {@value #MIN_TEMPERATURE} to {@value #MAX_TEMPERATURE} degrees in steps of
 *     {@value #TEMPERATURE_STEP};</li>
 *     <li>no window of the same heater on a day in common may overlap it - a disabled one included, so
 *     switching one on never makes two of them meet. With no overlap at most one schedule of a heater is on
 *     at a moment, and the order of the schedules no longer matters.</li>
 * </ul>
 */
public final class ScheduleRules {

    public static final double MIN_TEMPERATURE = 5;
    public static final double MAX_TEMPERATURE = 30;
    public static final double TEMPERATURE_STEP = 0.5;

    private static final Pattern MINUTES = Pattern.compile("([01]\\d|2[0-3]):[0-5]\\d");
    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    private ScheduleRules() {
    }

    /**
     * A body that keeps every rule, read into its values.
     *
     * @param others the stored schedules of the same heater, the one replaced left out
     */
    public static Valid check(final ScheduleBody body, final List<HeatingScheduleEntity> others) {
        if (body == null) {
            throw ScheduleException.invalid("a schedule is required");
        }
        final Valid valid = new Valid(
            days(body.days()),
            time("startTime", body.startTime()),
            time("endTime", body.endTime()),
            temperature(body.temperature())
        );
        if (!valid.startTime().isBefore(valid.endTime())) {
            throw ScheduleException.invalid("startTime must be before endTime - a window never runs across midnight");
        }
        others.stream()
            .filter(other -> overlaps(valid, other))
            .findFirst()
            .ifPresent(other -> {
                throw ScheduleException.invalid("the window overlaps schedule %d (%s %s-%s)".formatted(
                    other.id(), String.join(",", other.days()), other.startTime(), other.endTime()));
            });
        return valid;
    }

    //the days in the order of the week, the way the table and GET /rooms keep them
    public record Valid(List<DayOfWeek> days, LocalTime startTime, LocalTime endTime, BigDecimal temperature) {

        public List<String> dayNames() {
            return days.stream().map(DayOfWeek::name).toList();
        }
    }

    private static List<DayOfWeek> days(final List<String> days) {
        if (days == null || days.isEmpty()) {
            throw ScheduleException.invalid("days are required - at least one");
        }
        final Set<DayOfWeek> read = EnumSet.noneOf(DayOfWeek.class);
        for (final String day : days) {
            final DayOfWeek dayOfWeek = dayOf(day).orElseThrow(() -> ScheduleException.invalid(
                "day " + day + " is not a day of the week - one of " + Arrays.toString(DayOfWeek.values())));
            if (!read.add(dayOfWeek)) {
                throw ScheduleException.invalid("day " + day + " is listed twice");
            }
        }
        return List.copyOf(read);
    }

    //exactly the constant name: "monday" is refused like any other text, as the table would refuse it
    private static Optional<DayOfWeek> dayOf(final String day) {
        return Arrays.stream(DayOfWeek.values()).filter(dayOfWeek -> dayOfWeek.name().equals(day)).findFirst();
    }

    private static LocalTime time(final String name, final String value) {
        if (value == null) {
            throw ScheduleException.invalid(name + " is required");
        }
        if (!MINUTES.matcher(value).matches()) {
            throw ScheduleException.invalid(name + " must be HH:mm - " + value + " is not");
        }
        return LocalTime.parse(value);
    }

    private static BigDecimal temperature(final BigDecimal temperature) {
        if (temperature == null) {
            throw ScheduleException.invalid("temperature is required");
        }
        if (temperature.doubleValue() < MIN_TEMPERATURE || temperature.doubleValue() > MAX_TEMPERATURE) {
            throw ScheduleException.invalid(String.format(Locale.ROOT,
                "temperature must be from %.0f to %.0f degrees - %s is not", MIN_TEMPERATURE, MAX_TEMPERATURE, temperature.toPlainString()));
        }
        //a whole number of half degrees: twice the value has no fraction
        if (temperature.multiply(TWO).stripTrailingZeros().scale() > 0) {
            throw ScheduleException.invalid(String.format(Locale.ROOT,
                "temperature must be in steps of %.1f degrees - %s is not", TEMPERATURE_STEP, temperature.toPlainString()));
        }
        return temperature.setScale(1, RoundingMode.UNNECESSARY);
    }

    //a day in common and the windows meet: touching ends do not, both ends are exclusive
    private static boolean overlaps(final Valid valid, final HeatingScheduleEntity other) {
        final Set<String> otherDays = Set.copyOf(other.days());
        final boolean dayInCommon = valid.days().stream().map(DayOfWeek::name).anyMatch(otherDays::contains);
        return dayInCommon
            && valid.startTime().isBefore(other.endTime())
            && other.startTime().isBefore(valid.endTime());
    }
}
