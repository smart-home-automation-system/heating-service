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
 * {@code temperature} - the last reading of the room; after a start of the service the last one
 * stored. {@code updatedAt} is when this service received the reading - for one restored at a
 * start, the time stored with it. Missing only for a room that never reported.<br>
 * {@code humidity} - missing until a humidity is reported; nothing reports one today.<br>
 * {@code heatingEnabled} - whether any heater of the room was working at the end of the last pass
 * that got as far as its heaters. A pass that ends early leaves it as it was, and SANCTUM, whose
 * radiator is switched outside this service, never has one: there it is always false.
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
     * {@code inSchedule} - decided with the last reading the service received, so it is as old as
     * the {@code temperature} of the room - except after a start, when the temperature is restored
     * and nothing was decided yet: until the next reading it is false, as is
     * {@code heatingEnabled}. A schedule covered that moment <b>and</b> the room was colder
     * than the schedule asks for. A room that is warm enough is not "in schedule"; a room whose
     * sensor fell silent keeps the last decision, and the switch of the whole heating is not part
     * of it - with the heating off a heater can be "in schedule" and not working.<br>
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
     * {@code type} - what the temperature is a limit of: HEATING warms the room up to it.<br>
     * {@code days} - from Monday to Sunday.<br>
     * {@code startTime}, {@code endTime} - local time of the house; both ends are exclusive.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record HeaterSchedule(
        ScheduleType type,
        List<DayOfWeek> days,
        LocalTime startTime,
        LocalTime endTime,
        double temperature
    ) {
    }
}
