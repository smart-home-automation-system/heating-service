package cloud.cholewa.heating.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * A heating schedule as a caller writes it (HAS-356) - the body of a create and of a replace.<br>
 * {@code days} - constant names of {@code DayOfWeek} ({@code MONDAY} ... {@code SUNDAY}), as {@code GET /rooms}
 * lists them.<br>
 * {@code startTime}, {@code endTime} - {@code HH:mm}, local time of the house.<br>
 * {@code temperature} - degrees Celsius.<br>
 * Everything is required and nothing has a default; the text is read by {@code ScheduleRules}, which answers
 * what is wrong with it rather than a body that cannot be read.
 */
public record ScheduleBody(
    List<String> days,
    String startTime,
    String endTime,
    BigDecimal temperature
) {
}
