package cloud.cholewa.heating.service;

import cloud.cholewa.heating.db.model.TemperatureEntity;
import cloud.cholewa.heating.db.repository.TemperatureRepository;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.Temperature;
import cloud.cholewa.home.model.RoomName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomTemperatureSeederTest {

    private static final LocalDateTime STORED_AT = LocalDateTime.of(2026, 10, 8, 17, 55);

    private final HeaterActor radiator = HeaterActor.builder().type(HeaterType.RADIATOR).build();
    private final Room office = Room.builder()
        .name(RoomName.OFFICE)
        .temperature(Temperature.builder().build())
        .heaterActor(radiator)
        .build();
    private final Room livingRoom = Room.builder()
        .name(RoomName.LIVING_ROOM)
        .temperature(Temperature.builder().build())
        .build();

    @Mock
    private TemperatureRepository temperatureRepository;

    private RoomTemperatureSeeder sut;

    @BeforeEach
    void setUp() {
        sut = new RoomTemperatureSeeder(new Home(List.of(office, livingRoom)), temperatureRepository);

        lenient().when(temperatureRepository.findFirstByRoomOrderByDateDesc("office"))
            .thenReturn(Mono.just(new TemperatureEntity(1L, STORED_AT, "office", 20.4)));
        lenient().when(temperatureRepository.findFirstByRoomOrderByDateDesc("living room"))
            .thenReturn(Mono.just(new TemperatureEntity(2L, STORED_AT.minusDays(3), "living room", 18.9)));
    }

    //the time is the one of the measurement, not of the start: a reading of three days ago reads as that
    @Test
    void should_give_every_room_its_last_stored_temperature_with_the_time_it_was_measured_at() {
        sut.seed().as(StepVerifier::create).verifyComplete();

        assertThat(office.getTemperature().getValue()).isEqualTo(20.4);
        assertThat(office.getTemperature().getUpdatedAt()).isEqualTo(STORED_AT);
        assertThat(livingRoom.getTemperature().getValue()).isEqualTo(18.9);
        assertThat(livingRoom.getTemperature().getUpdatedAt()).isEqualTo(STORED_AT.minusDays(3));
    }

    @Test
    void should_leave_a_room_that_never_reported_without_a_temperature() {
        when(temperatureRepository.findFirstByRoomOrderByDateDesc("living room")).thenReturn(Mono.empty());

        sut.seed().as(StepVerifier::create).verifyComplete();

        assertThat(livingRoom.getTemperature().getUpdatedAt()).isNull();
        assertThat(office.getTemperature().getUpdatedAt()).isEqualTo(STORED_AT);
    }

    //the listener consumes while the rooms are seeded, and its reading is newer than any stored row
    @Test
    void should_not_replace_a_reading_that_arrived_in_the_meantime() {
        final LocalDateTime arrivedAt = STORED_AT.plusMinutes(4);
        office.getTemperature().setValue(21.1);
        office.getTemperature().setUpdatedAt(arrivedAt);

        sut.seed().as(StepVerifier::create).verifyComplete();

        assertThat(office.getTemperature().getValue()).isEqualTo(21.1);
        assertThat(office.getTemperature().getUpdatedAt()).isEqualTo(arrivedAt);
    }

    @Test
    void should_seed_the_other_rooms_when_one_cannot_be_read() {
        when(temperatureRepository.findFirstByRoomOrderByDateDesc("office"))
            .thenReturn(Mono.error(new IllegalStateException("connection lost")));

        sut.seed().as(StepVerifier::create).verifyComplete();

        assertThat(office.getTemperature().getUpdatedAt()).isNull();
        assertThat(livingRoom.getTemperature().getUpdatedAt()).isEqualTo(STORED_AT.minusDays(3));
    }

    //what a relay does is known only from the relay: a stored temperature says nothing about a heater
    @Test
    void should_not_touch_the_heaters() {
        sut.seed().as(StepVerifier::create).verifyComplete();

        assertThat(radiator.isWorking()).isFalse();
        assertThat(radiator.getLastStatusUpdate()).isNull();
        assertThat(radiator.isInSchedule()).isFalse();
        assertThat(radiator.getTargetTemperature()).isNull();
        assertThat(office.isRoomHeatingEnabled()).isFalse();
    }

    //the rooms are read when the seeding is subscribed to, one after the other
    @Test
    void should_read_nothing_before_it_is_subscribed_to() {
        final Mono<Void> seeding = sut.seed();

        assertThat(office.getTemperature().getUpdatedAt()).isNull();

        seeding.as(StepVerifier::create).verifyComplete();

        assertThat(office.getTemperature().getUpdatedAt()).isEqualTo(STORED_AT);
    }
}
