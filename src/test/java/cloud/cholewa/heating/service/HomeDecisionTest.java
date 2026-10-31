package cloud.cholewa.heating.service;

import cloud.cholewa.heating.calendar.HouseCalendarFixture;
import cloud.cholewa.heating.client.ShellyClient;
import cloud.cholewa.heating.infrastructure.error.BoilerException;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.HomeStatus;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.Schedule;
import cloud.cholewa.heating.model.Temperature;
import cloud.cholewa.heating.window.RoomWindow;
import cloud.cholewa.heating.window.WindowSensors;
import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.shelly.model.ShellyPro4StatusResponse;
import cloud.cholewa.shelly.model.ShellyProRelayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

//HAS-352: every room decided without a reading - the real schedule, heating and floor pump services, only the
//relays faked (they keep what they were last told). Monday 2 November 2026 in the house
class HomeDecisionTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private static final LocalDateTime MONDAY_0457 = LocalDateTime.of(2026, 11, 2, 4, 57);

    private final MovableClock clock = new MovableClock(MONDAY_0457.atZone(ZONE).toInstant());
    private final HomeStatus homeStatus = new HomeStatus();
    private final ShellyClient shellyClient = mock(ShellyClient.class);
    private final WindowSensors windowSensors = mock(WindowSensors.class);
    /** What each relay was last told, by room. */
    private final Map<RoomName, Boolean> relays = new HashMap<>();
    private final List<String> commands = new ArrayList<>();

    private Room tobi;
    private Room cinema;
    private Room livia;
    private HomeService sut;

    @BeforeEach
    void setUp() {
        when(windowSensors.windowOf(any())).thenReturn(RoomWindow.NO_SENSOR);
        when(shellyClient.getHeaterActorStatus(any(), any())).thenAnswer(call ->
            Mono.just(ShellyPro4StatusResponse.builder().output(relays.getOrDefault(call.getArgument(1), false)).build()));
        when(shellyClient.controlHeaterActor(any(), any(), anyBoolean())).thenAnswer(call -> {
            final RoomName room = call.getArgument(1);
            final boolean on = call.getArgument(2);
            relays.put(room, on);
            commands.add(room.getValue() + (on ? " on" : " off"));
            return Mono.just(ShellyProRelayResponse.builder().ison(on).build());
        });
        when(shellyClient.getFloorPumpStatus()).thenReturn(Mono.just(ShellyPro4StatusResponse.builder().output(false).build()));

        //tobi heats 05:00-08:00 on weekdays, as in the example of the owner
        tobi = room(RoomName.TOBI, 19.0, schedule(LocalTime.of(5, 0), LocalTime.of(8, 0), 21.0));
        cinema = room(RoomName.CINEMA, 18.0, schedule(LocalTime.of(0, 0), LocalTime.of(23, 59), 20.0));
        livia = room(RoomName.LIVIA, 22.0, schedule(LocalTime.of(0, 0), LocalTime.of(23, 59), 20.0));
        build(List.of(tobi, cinema, livia));
    }

    private void build(final List<Room> rooms) {
        final Home home = new Home(rooms);
        sut = new HomeService(clock, home, homeStatus, new ScheduleService(clock, HouseCalendarFixture.weekdays()),
            new HeatingService(clock, homeStatus, shellyClient, windowSensors),
            new FloorPumpService(clock, home, shellyClient));
    }

    @Test
    void should_command_every_room_below_its_target_and_in_schedule_in_one_pass_after_the_switch() {
        homeStatus.setEnabledHomeHeatingSystem(true);
        clock.set(MONDAY_0457.withHour(12));

        sut.decideRooms().as(StepVerifier::create).verifyComplete();

        //tobi is out of its schedule at noon, livia warm enough: only the cinema
        assertThat(commands).containsExactly("cinema on");
        assertThat(homeStatus.isAnyHeaterActive()).isTrue();
    }

    @Test
    void should_turn_every_working_relay_off_when_the_heating_is_switched_off() {
        homeStatus.setEnabledHomeHeatingSystem(true);
        clock.set(MONDAY_0457.withHour(6));
        sut.decideRooms().as(StepVerifier::create).verifyComplete();
        assertThat(commands).containsExactlyInAnyOrder("tobi on", "cinema on");

        homeStatus.setEnabledHomeHeatingSystem(false);
        commands.clear();
        sut.decideRooms().as(StepVerifier::create).verifyComplete();

        assertThat(commands).containsExactlyInAnyOrder("tobi off", "cinema off");
        assertThat(homeStatus.isAnyHeaterActive()).isFalse();
    }

    //the owner's example: a reading at 04:57 kept the radiator off and the next one came at 05:07 - now it
    //starts within the minute after 05:00 and stops within the minute after 08:00, with no reading at all.
    //One Mono subscribed again and again with the clock moved on, as the scheduler does
    @Test
    void should_act_on_a_boundary_of_a_schedule_within_the_minute_without_a_reading() {
        homeStatus.setEnabledHomeHeatingSystem(true);
        build(List.of(tobi));
        final var pass = sut.decideRooms();

        pass.as(StepVerifier::create).verifyComplete();
        assertThat(commands).isEmpty();

        clock.set(MONDAY_0457.withHour(5).withMinute(1));
        pass.as(StepVerifier::create).verifyComplete();
        assertThat(commands).containsExactly("tobi on");

        clock.set(MONDAY_0457.withHour(8).withMinute(1));
        pass.as(StepVerifier::create).verifyComplete();
        assertThat(commands).containsExactly("tobi on", "tobi off");
    }

    @Test
    void should_skip_a_room_without_a_temperature_yet() {
        homeStatus.setEnabledHomeHeatingSystem(true);
        clock.set(MONDAY_0457.withHour(12));
        final Room unread = Room.builder().name(RoomName.OFFICE)
            .temperature(Temperature.builder().build())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR)
                .schedule(schedule(LocalTime.of(0, 0), LocalTime.of(23, 59), 25.0)).build())
            .build();
        build(List.of(unread, cinema));

        sut.decideRooms().as(StepVerifier::create).verifyComplete();

        assertThat(commands).containsExactly("cinema on");
        assertThat(unread.isHeatingDecided()).isFalse();
    }

    @Test
    void should_decide_the_other_rooms_when_one_fails() {
        homeStatus.setEnabledHomeHeatingSystem(true);
        clock.set(MONDAY_0457.withHour(6));
        doReturn(Mono.error(new IllegalStateException("no relay of this room in the configuration")))
            .when(shellyClient).getHeaterActorStatus(HeaterType.RADIATOR, RoomName.TOBI);

        sut.decideRooms().as(StepVerifier::create).verifyComplete();

        assertThat(commands).containsExactly("cinema on");
    }

    //a relay that does not answer is skipped inside the room (HeatingService): the next rooms still decided
    @Test
    void should_decide_the_other_rooms_when_a_relay_does_not_answer() {
        homeStatus.setEnabledHomeHeatingSystem(true);
        clock.set(MONDAY_0457.withHour(6));
        //doReturn: stubbing through when() would call the answer of setUp, which records a command
        doReturn(Mono.error(new BoilerException("relay silent")))
            .when(shellyClient).controlHeaterActor(HeaterType.RADIATOR, RoomName.TOBI, true);

        sut.decideRooms().as(StepVerifier::create).verifyComplete();

        assertThat(commands).containsExactly("cinema on");
    }

    private static Room room(final RoomName name, final double temperature, final Schedule schedule) {
        return Room.builder()
            .name(name)
            .temperature(new Temperature(temperature, MONDAY_0457.minusMinutes(3)))
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR).schedule(schedule).build())
            .build();
    }

    private static Schedule schedule(final LocalTime from, final LocalTime to, final double temperature) {
        return Schedule.builder().day(DayOfWeek.MONDAY).startTime(from).endTime(to).temperature(temperature).build();
    }

    private static final class MovableClock extends Clock {
        private Instant now;

        MovableClock(final Instant now) {
            this.now = now;
        }

        void set(final LocalDateTime at) {
            now = at.atZone(ZONE).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return ZONE;
        }

        @Override
        public Clock withZone(final ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
