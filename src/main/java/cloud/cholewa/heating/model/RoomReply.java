package cloud.cholewa.heating.model;

import cloud.cholewa.home.model.RoomName;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * A room as a client sees it - a copy of the state held in memory, taken when it was asked for.
 * Whatever has not been measured or reported yet is left out, never filled with a default:<br>
 * {@code temperature} - missing until the first reading of the room since the service started.<br>
 * {@code humidity} - missing until a humidity is reported; nothing reports one today.<br>
 * {@code heatingEnabled} - true when any heater of the room was working after the last reading.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoomReply(
    RoomName name,
    RoomMode mode,
    boolean heatingEnabled,
    Reading temperature,
    Reading humidity,
    List<Heater> heaters
) {

    public record Reading(double value, LocalDateTime updatedAt) {
    }

    /**
     * {@code working} - what the relay last reported, missing until it has answered once.<br>
     * {@code updatedAt} - when it reported that; the relay is asked again with a reading of the
     * room, so a silent sensor or a relay that went away leaves both as they were.<br>
     * {@code inSchedule} - decided with the last reading of the room: a schedule covered that
     * moment <b>and</b> the room was colder than the schedule asks for. A room that is warm enough
     * is not "in schedule".<br>
     * {@code targetTemperature} - the temperature of that schedule, present only while
     * {@code inSchedule} is true.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Heater(
        HeaterType type,
        Boolean working,
        LocalDateTime updatedAt,
        boolean inSchedule,
        Double targetTemperature,
        List<HeaterSchedule> schedules
    ) {
    }

    /**
     * {@code days} - from Monday to Sunday.<br>
     * {@code startTime}, {@code endTime} - local time of the house; both ends are exclusive.
     */
    public record HeaterSchedule(
        List<DayOfWeek> days,
        LocalTime startTime,
        LocalTime endTime,
        double temperature
    ) {
    }
}
