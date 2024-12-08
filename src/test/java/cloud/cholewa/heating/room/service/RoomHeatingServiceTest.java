package cloud.cholewa.heating.room.service;

import cloud.cholewa.heating.model.BoilerRoom;
import cloud.cholewa.heating.model.Fireplace;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.Schedule;
import cloud.cholewa.heating.model.Temperature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static cloud.cholewa.heating.model.HeaterType.FLOOR;
import static cloud.cholewa.heating.model.HeaterType.RADIATOR;
import static cloud.cholewa.heating.model.HeatingTemperatures.FIREPLACE_ALERT_TEMPERATURE;
import static cloud.cholewa.heating.model.HeatingTemperatures.FIREPLACE_TEMPERATURE_ALLOW_DISABLE_HEATER;
import static cloud.cholewa.heating.model.HeatingTemperatures.FIREPLACE_TEMPERATURE_ALLOW_ENABLE_HEATER;
import static cloud.cholewa.home.model.RoomName.OFFICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoomHeatingServiceTest {

    private static final double ROOM_LOW_TEMPERATURE = 19;
    private static final double ROOM_HIGH_TEMPERATURE = 23;
    private static final double SCHEDULE_TEMPERATURE = 20.5;

    private BoilerRoom boilerRoom;
    private Fireplace fireplace;

    private RoomHeatingService sut;

    @BeforeEach
    void setUp() {
        fireplace = new Fireplace(new Temperature());
        boilerRoom = new BoilerRoom(null, fireplace, null, null);

        sut = new RoomHeatingService(boilerRoom, fireplace);
    }

    @Test
    void should_return_radiator_when_room_is_equipped_with_it() {
        assertNotNull(sut.getRadiatorActor(getRoomWithRadiator()));
    }

    @Test
    void should_throw_null_pointer_exception_when_room_is_not_equipped_with_radiator() {
        assertThrows(NullPointerException.class, () -> sut.getRadiatorActor(getRoomWithoutHeatingActors()));
    }

    @Test
    void should_return_floor_when_room_is_equipped_with_it() {
        assertNotNull(sut.getFloorActor(getRoomWithFloor()));
    }

    @Test
    void should_throw_null_pointer_exception_when_room_is_not_equipped_with_floor() {
        assertThrows(NullPointerException.class, () -> sut.getRadiatorActor(getRoomWithoutHeatingActors()));
    }

    @Test
    void should_return_floor_and_radiator_when_room_is_equipped_with_it() {
        assertNotNull(sut.getRadiatorActor(getRoomWithBothTypesOfHeatingActors()));
        assertNotNull(sut.getFloorActor(getRoomWithBothTypesOfHeatingActors()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("heating_scenarios")
    void should_control_heaters(
        final String name,
        final double roomTemperature,
        final boolean isRoomTemperatureUpdated,
        final Room room,
        final Schedule schedule,
        final HeaterActor heaterActor,
        final double fireplaceTemperature,
        final boolean isHeatingAllowed,
        final boolean shouldTurnOnHeaterByFireplaceAlert,
        final boolean shouldTurnOnRadiatorBySchedule,
        final boolean shouldTurnOnRadiatorByFireplace,
        final boolean shouldTurnOffHeater
    ) {
        boilerRoom.setHeatingEnabled(isHeatingAllowed);
        fireplace.temperature().setValue(fireplaceTemperature);
        room.getTemperature().setValue(roomTemperature);
        if (isRoomTemperatureUpdated) {
            room.getTemperature().setUpdatedAt(LocalDateTime.now());
        }

        assertEquals(shouldTurnOnHeaterByFireplaceAlert, sut.shouldTurnOnHeaterByFireplaceAlert(heaterActor));
        assertEquals(shouldTurnOnRadiatorBySchedule, sut.shouldTurnOnRadiatorBySchedule(room, schedule, heaterActor));
        assertEquals(shouldTurnOnRadiatorByFireplace, sut.shouldTurnOnRadiatorByFireplace(room, heaterActor));
        assertEquals(shouldTurnOffHeater, sut.shouldTurnOffHeater(room, schedule, heaterActor));
    }

    private static Stream<Arguments> heating_scenarios() {
        return Stream.of(
            Arguments.of(
                "fireplace alert active other conditions are irrelevant but room heater disabled",
                ROOM_HIGH_TEMPERATURE, true,
                Room.builder()
                    .temperature(new Temperature())
                    .build(),
                Schedule.builder().temperature(SCHEDULE_TEMPERATURE).build(),
                HeaterActor.builder().working(false).build(),
                FIREPLACE_ALERT_TEMPERATURE, true,
                true, false, false, false
            ),
            Arguments.of(
                "fireplace alert active other conditions are irrelevant but room heater enabled",
                ROOM_HIGH_TEMPERATURE, true,
                Room.builder()
                    .temperature(new Temperature())
                    .build(),
                Schedule.builder().temperature(SCHEDULE_TEMPERATURE).build(),
                HeaterActor.builder().working(true).build(),
                FIREPLACE_ALERT_TEMPERATURE, true,
                false, false, false, false
            ),
            Arguments.of(
                "no schedule, room temperature low, fireplace on, home heating disabled, room heater disabled",
                ROOM_LOW_TEMPERATURE, true,
                Room.builder()
                    .temperature(new Temperature())
                    .build(),
                null,
                HeaterActor.builder().build(),
                FIREPLACE_TEMPERATURE_ALLOW_ENABLE_HEATER, false,
                false, false, true, false
            ),
            Arguments.of(
                "no schedule, room temperature high, fireplace off, home heating disabled, room heater disabled",
                ROOM_HIGH_TEMPERATURE, true,
                Room.builder()
                    .temperature(new Temperature())
                    .build(),
                null,
                HeaterActor.builder().build(),
                FIREPLACE_TEMPERATURE_ALLOW_ENABLE_HEATER, false,
                false, false, false, false
            ),
            Arguments.of(
                "no schedule, room temperature high, fireplace off, home heating disabled, room heater enabled",
                ROOM_HIGH_TEMPERATURE, true,
                Room.builder()
                    .temperature(new Temperature())
                    .build(),
                null,
                HeaterActor.builder().working(true).build(),
                FIREPLACE_TEMPERATURE_ALLOW_DISABLE_HEATER, false,
                false, false, false, true
            ),
            Arguments.of(
                "no schedule, room temperature low, fireplace off, home heating disabled, room heater enabled",
                ROOM_HIGH_TEMPERATURE, true,
                Room.builder()
                    .temperature(new Temperature())
                    .build(),
                null,
                HeaterActor.builder().working(true).build(),
                FIREPLACE_TEMPERATURE_ALLOW_DISABLE_HEATER, false,
                false, false, false, true
            ),
            Arguments.of(
                "no schedule, room temperature low, fireplace off, home heating disabled, room heater disabled",
                ROOM_LOW_TEMPERATURE, true,
                Room.builder()
                    .temperature(new Temperature())
                    .build(),
                null,
                HeaterActor.builder().working(false).build(),
                FIREPLACE_TEMPERATURE_ALLOW_DISABLE_HEATER, false,
                false, false, false, false
            ),
            Arguments.of(
                "schedule, room temperature low, fireplace on, home heating disabled, room heater disabled",
                ROOM_LOW_TEMPERATURE, true,
                Room.builder()
                    .temperature(new Temperature())
                    .build(),
                Schedule.builder().temperature(SCHEDULE_TEMPERATURE).build(),
                HeaterActor.builder().working(false).build(),
                FIREPLACE_TEMPERATURE_ALLOW_ENABLE_HEATER, false,
                false, false, true, false
            ),
            Arguments.of(
                "schedule, room temperature low, fireplace on, home heating disabled, room heater enabled",
                ROOM_LOW_TEMPERATURE, true,
                Room.builder()
                    .temperature(new Temperature())
                    .build(),
                Schedule.builder().temperature(SCHEDULE_TEMPERATURE).build(),
                HeaterActor.builder().working(true).build(),
                FIREPLACE_TEMPERATURE_ALLOW_ENABLE_HEATER, false,
                false, false, false, false
            ),
            Arguments.of(
                "schedule, room temperature high, fireplace on, home heating disabled, room heater disabled",
                ROOM_HIGH_TEMPERATURE, true,
                Room.builder()
                    .temperature(new Temperature())
                    .build(),
                Schedule.builder().temperature(SCHEDULE_TEMPERATURE).build(),
                HeaterActor.builder().working(false).build(),
                FIREPLACE_TEMPERATURE_ALLOW_ENABLE_HEATER, false,
                false, false, false, false
            ),Arguments.of(
                "schedule, room temperature low, fireplace off, home heating disabled, room heater disabled",
                ROOM_LOW_TEMPERATURE, true,
                Room.builder()
                    .temperature(new Temperature())
                    .build(),
                Schedule.builder().temperature(SCHEDULE_TEMPERATURE).build(),
                HeaterActor.builder().working(false).build(),
                FIREPLACE_TEMPERATURE_ALLOW_DISABLE_HEATER, false,
                false, false, false, false
            ),
            Arguments.of(
                "schedule, room temperature low, fireplace off, home heating disabled, room heater enabled",
                ROOM_LOW_TEMPERATURE, true,
                Room.builder()
                    .temperature(new Temperature())
                    .build(),
                Schedule.builder().temperature(SCHEDULE_TEMPERATURE).build(),
                HeaterActor.builder().working(true).build(),
                FIREPLACE_TEMPERATURE_ALLOW_DISABLE_HEATER, false,
                false, false, false, true
            ),
            Arguments.of(
                "schedule, room temperature high, fireplace off, home heating disabled, room heater enabled",
                ROOM_LOW_TEMPERATURE, true,
                Room.builder()
                    .temperature(new Temperature())
                    .build(),
                Schedule.builder().temperature(SCHEDULE_TEMPERATURE).build(),
                HeaterActor.builder().working(true).build(),
                FIREPLACE_TEMPERATURE_ALLOW_DISABLE_HEATER, false,
                false, false, false, true
            ),
            Arguments.of(
                "schedule, room temperature low, fireplace off, home heating disabled, room heater disabled",
                ROOM_LOW_TEMPERATURE, true,
                Room.builder()
                    .temperature(new Temperature())
                    .build(),
                Schedule.builder().temperature(SCHEDULE_TEMPERATURE).build(),
                HeaterActor.builder().working(false).build(),
                FIREPLACE_TEMPERATURE_ALLOW_DISABLE_HEATER, false,
                false, false, false, false
            ),
            Arguments.of(
                "schedule, room temperature low, fireplace off, home heating enabled, room heater disabled",
                ROOM_LOW_TEMPERATURE, true,
                Room.builder()
                    .temperature(new Temperature())
                    .build(),
                Schedule.builder().temperature(SCHEDULE_TEMPERATURE).build(),
                HeaterActor.builder().working(false).build(),
                FIREPLACE_TEMPERATURE_ALLOW_DISABLE_HEATER, true,
                false, true, false, false
            ),
            Arguments.of(
                "schedule, room temperature high, fireplace off, home heating enabled, room heater disabled",
                ROOM_HIGH_TEMPERATURE, true,
                Room.builder()
                    .temperature(new Temperature())
                    .build(),
                Schedule.builder().temperature(SCHEDULE_TEMPERATURE).build(),
                HeaterActor.builder().working(false).build(),
                FIREPLACE_TEMPERATURE_ALLOW_DISABLE_HEATER, true,
                false, false, false, false
            ),
            Arguments.of(
                "schedule, room temperature high, fireplace off, home heating disabled, room heater enabled",
                ROOM_HIGH_TEMPERATURE, true,
                Room.builder()
                    .temperature(new Temperature())
                    .build(),
                Schedule.builder().temperature(SCHEDULE_TEMPERATURE).build(),
                HeaterActor.builder().working(true).build(),
                FIREPLACE_TEMPERATURE_ALLOW_DISABLE_HEATER, false,
                false, false, false, true
            )
        );
    }

    private Room getRoomWithoutHeatingActors() {
        return Room.builder()
            .name(OFFICE)
            .build();
    }

    private Room getRoomWithRadiator() {
        return Room.builder()
            .name(OFFICE)
            .heaterActors(List.of(
                HeaterActor.builder().name(RADIATOR).build()
            ))
            .build();
    }

    private Room getRoomWithFloor() {
        return Room.builder()
            .name(OFFICE)
            .heaterActors(List.of(
                HeaterActor.builder().name(FLOOR).build()
            ))
            .build();
    }

    private Room getRoomWithBothTypesOfHeatingActors() {
        return Room.builder()
            .name(OFFICE)
            .heaterActors(List.of(
                HeaterActor.builder().name(RADIATOR).build(),
                HeaterActor.builder().name(FLOOR).build()
            ))
            .build();
    }
}