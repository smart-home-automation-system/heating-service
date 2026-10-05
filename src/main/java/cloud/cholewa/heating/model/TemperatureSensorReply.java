package cloud.cholewa.heating.model;

import cloud.cholewa.home.model.RoomName;

import java.time.LocalDateTime;

/**
 * {@code lastReadingAt} - the date of the last temperature stored for the room.<br>
 * {@code stale} - true when the sensor has been silent for longer than the configured limit.<br>
 * {@code muted} - true when the room is excluded from the alerts; {@code stale} is still reported.
 */
public record TemperatureSensorReply(
    RoomName room,
    LocalDateTime lastReadingAt,
    boolean stale,
    boolean muted
) {
}
