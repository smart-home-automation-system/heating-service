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
 * that got as far as its heaters; a pass that ends early leaves it as it was. Missing until such
 * a pass has run since the service started - for good in a room without a heater, and in
 * SANCTUM, whose radiator is switched outside this service.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoomReply(
    RoomName name,
    RoomMode mode,
    Boolean heatingEnabled,
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
     * {@code inSchedule} - decided with the last reading the service received, and missing until
     * one has been since the service started: a temperature restored at a start decides nothing.
     * A schedule covered that moment <b>and</b> the room was colder
     * than the schedule asks for. A room that is warm enough is not "in schedule"; a room whose
     * sensor fell silent keeps the last decision, and the switch of the whole heating is not part
     * of it - with the heating off a heater can be "in schedule" and not working.<br>
     * {@code targetTemperature} - the temperature of that schedule, present only while
     * {@code inSchedule} is true.<br>
     * {@code scheduledTemperature} - what the schedules ask for at the moment of the question,
     * whether or not the room has reached it: the highest temperature of the schedules that are
     * on now, missing when none is. Worked out when asked, by the clock of the house - it needs
     * no reading and is there from the start of the service. It is the figure to show as the
     * target of a room; {@code targetTemperature} disappears as soon as the room is warm enough.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Heater(
        HeaterType type,
        Boolean working,
        LocalDateTime updatedAt,
        Boolean inSchedule,
        Double targetTemperature,
        Double scheduledTemperature,
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
