package cloud.cholewa.heating.model;

import cloud.cholewa.home.model.RoomName;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * A heating schedule as stored, the answer of a create and of a replace (HAS-356). The fields are those of
 * a schedule in {@code GET /rooms} - the times written the same way - plus the room and the heater it
 * belongs to. The dashboard reads the rooms again after a write; this answer is for a caller like Bruno.
 */
public record StoredSchedule(
    long id,
    RoomName room,
    HeaterType heater,
    List<DayOfWeek> days,
    LocalTime startTime,
    LocalTime endTime,
    double temperature,
    boolean enabled
) {
}
