package cloud.cholewa.heating.window;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The windows and doors of one room (HAS-232), judged by every opening sensor the registry places
 * in that room: {@code open} - one reports open; {@code closed} - every one reports closed;
 * {@code no sensor} - the room has none and heats as it always did; {@code unknown} - a sensor never
 * reported since the start, or its gateway has been silent too long, and none is open. The values
 * are those of the window of a blind in blinds-service (HAS-290) - a contract the dashboard copies.
 */
public enum WindowState {
    CLOSED("closed"),
    OPEN("open"),
    NO_SENSOR("no sensor"),
    UNKNOWN("unknown");

    private final String value;

    WindowState(final String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
