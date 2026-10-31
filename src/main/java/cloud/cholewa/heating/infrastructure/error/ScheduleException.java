package cloud.cholewa.heating.infrastructure.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

//A write of a heating schedule that was refused (HAS-356): the status says whose problem it is, the error
//names the cause and the message is the reason, answered as the details - every one of them is the
//caller's, none quotes a device or the database
@Getter
public class ScheduleException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final HeatingErrorId error;

    private ScheduleException(final HttpStatus httpStatus, final HeatingErrorId error, final String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.error = error;
    }

    public static ScheduleException invalid(final String reason) {
        return new ScheduleException(HttpStatus.BAD_REQUEST, HeatingErrorId.INVALID_SCHEDULE, reason);
    }

    public static ScheduleException notFound(final long id) {
        return new ScheduleException(HttpStatus.NOT_FOUND, HeatingErrorId.NOT_FOUND_SCHEDULE, "no schedule with id " + id);
    }

    public static ScheduleException heaterNotFound(final String room, final String heater) {
        return new ScheduleException(
            HttpStatus.NOT_FOUND,
            HeatingErrorId.NOT_FOUND_HEATER,
            "room " + room + " has no " + heater + " this service drives"
        );
    }
}
