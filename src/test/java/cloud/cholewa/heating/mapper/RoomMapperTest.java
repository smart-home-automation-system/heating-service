package cloud.cholewa.heating.mapper;

import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.heating.model.Humidity;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.RoomMode;
import cloud.cholewa.heating.model.RoomReply;
import cloud.cholewa.heating.model.Schedule;
import cloud.cholewa.heating.model.ScheduleType;
import cloud.cholewa.heating.model.Temperature;
import cloud.cholewa.home.model.RoomName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

class RoomMapperTest {

    private static final LocalDateTime READING_AT = LocalDateTime.of(2026, 10, 8, 18, 30);
    private static final LocalDateTime RELAY_AT = LocalDateTime.of(2026, 10, 8, 18, 31);

    private final RoomMapper sut = new RoomMapper();

    //the room exactly as HomeConfig builds it, before anything was measured or reported
    @Test
    void should_leave_out_what_was_never_measured_or_reported() {
        final Room room = Room.builder()
            .name(RoomName.OFFICE)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR).build())
            .build();

        final RoomReply reply = sut.toReply(room);

        assertThat(reply.name()).isEqualTo(RoomName.OFFICE);
        assertThat(reply.mode()).isEqualTo(RoomMode.HEATING);
        assertThat(reply.heatingEnabled()).isFalse();
        assertThat(reply.temperature()).isNull();
        assertThat(reply.humidity()).isNull();
        assertThat(reply.heaters()).singleElement().satisfies(heater -> {
            assertThat(heater.type()).isEqualTo(HeaterType.RADIATOR);
            assertThat(heater.working()).isNull();
            assertThat(heater.updatedAt()).isNull();
            assertThat(heater.inSchedule()).isFalse();
            assertThat(heater.targetTemperature()).isNull();
            assertThat(heater.schedules()).isEmpty();
        });
    }

    //the loft: no humidity object at all and no heater
    @Test
    void should_map_a_room_without_humidity_and_heaters() {
        final Room room = Room.builder()
            .name(RoomName.LOFT)
            .temperature(Temperature.builder().build())
            .build();

        final RoomReply reply = sut.toReply(room);

        assertThat(reply.temperature()).isNull();
        assertThat(reply.humidity()).isNull();
        assertThat(reply.heaters()).isEmpty();
    }

    @Test
    void should_map_the_state_of_a_room() {
        final Humidity humidity = new Humidity();
        humidity.setValue(48);
        humidity.setUpdatedAt(READING_AT);

        final HeaterActor radiator = HeaterActor.builder()
            .type(HeaterType.RADIATOR)
            .schedule(Schedule.builder().type(ScheduleType.HEATING)
                .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
                .temperature(20.5)
                .startTime(LocalTime.of(7, 0))
                .endTime(LocalTime.of(23, 0))
                .build())
            .build();
        radiator.setWorking(true);
        radiator.setLastStatusUpdate(RELAY_AT);
        radiator.setInSchedule(true);
        radiator.setTargetTemperature(20.5);

        final Room room = Room.builder()
            .name(RoomName.LIVING_ROOM)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().value(19.4).updatedAt(READING_AT).build())
            .humidity(humidity)
            .heaterActor(radiator)
            .build();
        room.setRoomHeatingEnabled(true);

        final RoomReply reply = sut.toReply(room);

        assertThat(reply.heatingEnabled()).isTrue();
        assertThat(reply.temperature()).isEqualTo(new RoomReply.Reading(19.4, READING_AT));
        assertThat(reply.humidity()).isEqualTo(new RoomReply.Reading(48, READING_AT));
        assertThat(reply.heaters()).containsExactly(new RoomReply.Heater(
            HeaterType.RADIATOR,
            true,
            RELAY_AT,
            true,
            20.5,
            List.of(new RoomReply.HeaterSchedule(
                ScheduleType.HEATING,
                List.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY),
                LocalTime.of(7, 0),
                LocalTime.of(23, 0),
                20.5
            ))
        ));
    }

    //a relay that answered "off" is known to be off - that is not the same as never having answered
    @Test
    void should_report_a_heater_that_answered_off_as_not_working() {
        final HeaterActor floor = HeaterActor.builder().type(HeaterType.FLOOR).build();
        floor.setLastStatusUpdate(RELAY_AT);

        final Room room = Room.builder().name(RoomName.WARDROBE).heaterActor(floor).build();

        assertThat(sut.toReply(room).heaters()).singleElement().satisfies(heater -> {
            assertThat(heater.working()).isFalse();
            assertThat(heater.updatedAt()).isEqualTo(RELAY_AT);
        });
    }

    //the two are written one after the other by the listener; a reply caught in between must not
    //claim a schedule without a target, nor a target without a schedule
    @Test
    void should_never_report_a_target_without_in_schedule_nor_the_other_way_round() {
        final HeaterActor withoutTarget = HeaterActor.builder().type(HeaterType.RADIATOR).build();
        withoutTarget.setInSchedule(true);

        final HeaterActor withoutSchedule = HeaterActor.builder().type(HeaterType.FLOOR).build();
        withoutSchedule.setTargetTemperature(21.0);

        final Room room = Room.builder()
            .name(RoomName.BATHROOM_UP)
            .heaterActor(withoutTarget)
            .heaterActor(withoutSchedule)
            .build();

        assertThat(sut.toReply(room).heaters()).hasSize(2).allSatisfy(heater -> {
            assertThat(heater.inSchedule()).isFalse();
            assertThat(heater.targetTemperature()).isNull();
        });
    }

    //Set.of iterates in an order that changes between starts of the JVM, whatever order it was written in
    @Test
    void should_list_the_days_of_a_schedule_from_monday_to_sunday() {
        final Room room = Room.builder()
            .name(RoomName.OFFICE)
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR)
                .schedule(Schedule.builder()
                    .days(Set.of(SUNDAY, WEDNESDAY, MONDAY, SATURDAY, FRIDAY, TUESDAY, THURSDAY))
                    .startTime(LocalTime.of(7, 0))
                    .endTime(LocalTime.of(23, 0))
                    .build())
                .build())
            .build();

        assertThat(sut.toReply(room).heaters().getFirst().schedules().getFirst().days())
            .containsExactly(DayOfWeek.values());
    }
}
