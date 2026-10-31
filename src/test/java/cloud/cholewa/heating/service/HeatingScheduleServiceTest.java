package cloud.cholewa.heating.service;

import cloud.cholewa.heating.config.RelayConfig;
import cloud.cholewa.heating.db.model.HeatingScheduleEntity;
import cloud.cholewa.heating.db.repository.HeatingScheduleRepository;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.Schedule;
import cloud.cholewa.home.model.RoomName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SUNDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeatingScheduleServiceTest {

    @Mock
    private HeatingScheduleRepository repository;

    private HeaterActor livingRoomRadiator;
    private HeaterActor livingRoomFloor;
    private HeaterActor officeRadiator;
    private HeaterActor sanctumRadiator;
    private HeatingScheduleService sut;

    @BeforeEach
    void setUp() {
        livingRoomRadiator = HeaterActor.builder().type(HeaterType.RADIATOR).build();
        livingRoomFloor = HeaterActor.builder().type(HeaterType.FLOOR).build();
        officeRadiator = HeaterActor.builder().type(HeaterType.RADIATOR).build();
        sanctumRadiator = HeaterActor.builder().type(HeaterType.RADIATOR).build();
        final Home home = new Home(List.of(
            Room.builder().name(RoomName.LIVING_ROOM).heaterActor(livingRoomRadiator).heaterActor(livingRoomFloor).build(),
            Room.builder().name(RoomName.OFFICE).heaterActor(officeRadiator).build(),
            Room.builder().name(RoomName.SANCTUM).heaterActor(sanctumRadiator).build(),
            Room.builder().name(RoomName.LOFT).build()
        ));
        //as in application.yaml: no relay of SANCTUM, whose radiator is switched outside the service
        final RelayConfig relayConfig = new RelayConfig();
        relayConfig.setHeater(Map.of("living_room", 1, "office", 3));
        relayConfig.setFloor(Map.of("living_room", 0));
        sut = new HeatingScheduleService(home, repository, relayConfig);
    }

    @Test
    void should_give_every_heater_its_schedules_in_the_order_they_are_stored() {
        when(repository.findAllByOrderByIdAsc()).thenReturn(Flux.just(
            row(1L, "living room", "radiator", List.of("MONDAY"), 6, 8, "21.0", true),
            row(2L, "living room", "floor", List.of("SUNDAY", "MONDAY"), 15, 22, "20.5", false),
            row(3L, "living room", "radiator", List.of("SUNDAY"), 8, 22, "19.5", true)
        ));

        sut.load().as(StepVerifier::create).verifyComplete();

        assertThat(livingRoomRadiator.getSchedules()).extracting(Schedule::getId).containsExactly(1L, 3L);
        assertThat(livingRoomRadiator.getSchedules().getFirst()).satisfies(schedule -> {
            assertThat(schedule.getDays()).containsExactly(MONDAY);
            assertThat(schedule.getStartTime()).isEqualTo(LocalTime.of(6, 0));
            assertThat(schedule.getEndTime()).isEqualTo(LocalTime.of(8, 0));
            assertThat(schedule.getTemperature()).isEqualTo(21.0);
            assertThat(schedule.isEnabled()).isTrue();
        });
        assertThat(livingRoomFloor.getSchedules()).singleElement().satisfies(schedule -> {
            assertThat(schedule.getDays()).containsExactlyInAnyOrder(SUNDAY, MONDAY);
            assertThat(schedule.getTemperature()).isEqualTo(20.5);
            assertThat(schedule.isEnabled()).isFalse();
        });
        //a heater without a row has no schedule
        assertThat(officeRadiator.getSchedules()).isEmpty();
    }

    //one row the house cannot place must not cost the others their schedules
    @Test
    void should_leave_out_a_row_this_version_cannot_place_and_load_the_others() {
        when(repository.findAllByOrderByIdAsc()).thenReturn(Flux.just(
            row(1L, "cellar", "radiator", List.of("MONDAY"), 6, 8, "21.0", true),
            row(2L, "office", "floor", List.of("MONDAY"), 6, 8, "21.0", true),
            row(3L, "office", "towel rail", List.of("MONDAY"), 6, 8, "21.0", true),
            row(4L, "office", "radiator", List.of("MONDAY", "FUNDAY"), 6, 8, "21.0", true),
            row(5L, "loft", "radiator", List.of("MONDAY"), 6, 8, "21.0", true),
            row(6L, "office", "radiator", List.of("MONDAY"), 7, 23, "20.5", true),
            //a heater of the house the service does not drive: a schedule would make the loop drive it
            row(7L, "sanctum", "radiator", List.of("MONDAY"), 7, 23, "20.5", true)
        ));

        sut.load().as(StepVerifier::create).verifyComplete();

        assertThat(officeRadiator.getSchedules()).extracting(Schedule::getId).containsExactly(6L);
        assertThat(livingRoomRadiator.getSchedules()).isEmpty();
        assertThat(livingRoomFloor.getSchedules()).isEmpty();
        assertThat(sanctumRadiator.getSchedules()).isEmpty();
    }

    //read again, the schedules of a heater are what is stored now - a removed one is gone
    @Test
    void should_replace_the_schedules_a_heater_had() {
        when(repository.findAllByOrderByIdAsc())
            .thenReturn(Flux.just(row(1L, "office", "radiator", List.of("MONDAY"), 7, 23, "20.5", true)))
            .thenReturn(Flux.empty());

        sut.load().as(StepVerifier::create).verifyComplete();
        assertThat(officeRadiator.getSchedules()).hasSize(1);

        sut.load().as(StepVerifier::create).verifyComplete();
        assertThat(officeRadiator.getSchedules()).isEmpty();
    }

    //a failed read changes nothing: the heaters keep what they had
    @Test
    void should_keep_the_schedules_when_the_database_fails() {
        officeRadiator.replaceSchedules(List.of(Schedule.builder().id(9L).build()));
        when(repository.findAllByOrderByIdAsc()).thenReturn(Flux.error(new IllegalStateException("database down")));

        sut.load().as(StepVerifier::create).verifyError(IllegalStateException.class);

        assertThat(officeRadiator.getSchedules()).extracting(Schedule::getId).containsExactly(9L);
    }

    private static HeatingScheduleEntity row(
        final Long id,
        final String room,
        final String heater,
        final List<String> days,
        final int from,
        final int to,
        final String temperature,
        final boolean enabled
    ) {
        return new HeatingScheduleEntity(
            id, room, heater, days, LocalTime.of(from, 0), LocalTime.of(to, 0), new BigDecimal(temperature), enabled, null, null
        );
    }
}
