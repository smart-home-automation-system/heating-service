package cloud.cholewa.heating.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

//The rules of the range a history is asked for - the ones of the reports of presence-service, with
//a limit of its own - and the width of the buckets it is answered in.
final class HistoryRange {

    static final long MAX_DAYS = 31;

    //LocalDateTime holds years the timestamp of the database does not (it ends at 294276), and a
    //short range out there would pass the rules below and fail in the query, as a 500
    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 9999;

    //Sized by the sensors (HAS-199, production data of 2026-10-09): they report every 42 s to
    //16 min, so a bucket below 16 min would leave holes in the line of the slow ones. Each width
    //divides a day, which is what makes a bucket start on the clock of the house.
    private static final Duration SHORT_RANGE = Duration.ofDays(2);
    private static final Duration SHORT_RANGE_BUCKET = Duration.ofMinutes(20);
    private static final Duration MEDIUM_RANGE = Duration.ofDays(8);
    private static final Duration MEDIUM_RANGE_BUCKET = Duration.ofHours(1);
    private static final Duration LONG_RANGE_BUCKET = Duration.ofHours(3);

    private HistoryRange() {
    }

    //what is wrong with the range, as the 400 to answer - empty for a valid one
    static Optional<ResponseStatusException> violation(final LocalDateTime from, final LocalDateTime to) {
        if (isOutsideTheCalendar(from) || isOutsideTheCalendar(to)) {
            return Optional.of(new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "from and to must be within the years " + MIN_YEAR + " to " + MAX_YEAR
            ));
        }
        if (!from.isBefore(to)) {
            return Optional.of(new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before to"));
        }
        if (isLongerThanLimit(from, to)) {
            return Optional.of(new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "The range must not be longer than " + MAX_DAYS + " days"));
        }
        return Optional.empty();
    }

    //The width of the buckets of a valid range: 144, 192 and 248 points at most for a range that
    //starts on a bucket, one more for one that does not - its first bucket starts before "from"
    static Duration bucket(final LocalDateTime from, final LocalDateTime to) {
        final Duration length = Duration.between(from, to);

        if (length.compareTo(SHORT_RANGE) <= 0) {
            return SHORT_RANGE_BUCKET;
        }
        return length.compareTo(MEDIUM_RANGE) <= 0 ? MEDIUM_RANGE_BUCKET : LONG_RANGE_BUCKET;
    }

    private static boolean isOutsideTheCalendar(final LocalDateTime bound) {
        return bound.getYear() < MIN_YEAR || bound.getYear() > MAX_YEAR;
    }

    //Counted in calendar days on the local dates, with the time of day deciding a range of exactly
    //the limit. Not from.plusDays: the bounds come straight from the request, and adding to a date
    //at the edge of what LocalDateTime holds throws instead of answering 400
    private static boolean isLongerThanLimit(final LocalDateTime from, final LocalDateTime to) {
        final long days = to.toLocalDate().toEpochDay() - from.toLocalDate().toEpochDay();

        return days > MAX_DAYS || (days == MAX_DAYS && to.toLocalTime().isAfter(from.toLocalTime()));
    }
}
