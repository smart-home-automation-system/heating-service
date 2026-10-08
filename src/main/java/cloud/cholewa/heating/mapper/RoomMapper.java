package cloud.cholewa.heating.mapper;

import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.Humidity;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.RoomReply;
import cloud.cholewa.heating.model.Schedule;
import cloud.cholewa.heating.model.Temperature;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

//Written by hand, not by MapStruct: the point of the reply is what it leaves out. The state in
//memory starts with defaults - a temperature of 0.0, a heater that is "not working" - and only
//the timestamp next to a value tells whether it was ever measured or reported
@Component
public class RoomMapper {

    public RoomReply toReply(final Room room) {
        return new RoomReply(
            room.getName(),
            room.getMode(),
            room.isRoomHeatingEnabled(),
            toReading(room.getTemperature()),
            toReading(room.getHumidity()),
            room.getHeaterActors().stream().map(this::toHeater).toList()
        );
    }

    private RoomReply.Reading toReading(final Temperature temperature) {
        if (temperature == null) {
            return null;
        }
        //a reading is written as one (HomeService), so it is read as one
        final Temperature reading = temperature.snapshot();
        return toReading(reading.getValue(), reading.getUpdatedAt());
    }

    private RoomReply.Reading toReading(final Humidity humidity) {
        return humidity == null ? null : toReading(humidity.getValue(), humidity.getUpdatedAt());
    }

    private RoomReply.Reading toReading(final double value, final LocalDateTime updatedAt) {
        return updatedAt == null ? null : new RoomReply.Reading(value, updatedAt);
    }

    private RoomReply.Heater toHeater(final HeaterActor heaterActor) {
        final LocalDateTime updatedAt = heaterActor.getLastStatusUpdate();
        //the listener writes the two one after the other while this thread reads them, so each is read
        //once and the reply is made to agree with itself: a target only with "in schedule", and both or none
        final Double targetTemperature = heaterActor.getTargetTemperature();
        final boolean inSchedule = heaterActor.isInSchedule() && targetTemperature != null;

        return new RoomReply.Heater(
            heaterActor.getType(),
            updatedAt == null ? null : heaterActor.isWorking(),
            updatedAt,
            inSchedule,
            inSchedule ? targetTemperature : null,
            heaterActor.getSchedules().stream().map(this::toSchedule).toList()
        );
    }

    private RoomReply.HeaterSchedule toSchedule(final Schedule schedule) {
        return new RoomReply.HeaterSchedule(
            schedule.getType(),
            //the days are a set, and the order of a Set.of differs from one start of the JVM to the next
            schedule.getDays().stream().sorted().toList(),
            schedule.getStartTime(),
            schedule.getEndTime(),
            schedule.getTemperature()
        );
    }
}
