package cloud.cholewa.heating.service;

import cloud.cholewa.heating.config.SensorMonitorProperties;
import cloud.cholewa.heating.db.model.TemperatureEntity;
import cloud.cholewa.heating.db.repository.TemperatureRepository;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.TemperatureSensorReply;
import cloud.cholewa.home.model.RoomName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemperatureSensorServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 12, 0);

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private TemperatureRepository temperatureRepository;

    private TemperatureSensorService sut;

    @BeforeEach
    void setUp() {
        sut = new TemperatureSensorService(
            Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE),
            new Home(List.of(room(RoomName.OFFICE), room(RoomName.GARAGE), room(RoomName.LOFT))),
            new SensorMonitorProperties(
                "0 0 * * * *", Duration.ofHours(24), Duration.ofHours(24), Set.of(RoomName.GARAGE)),
            temperatureRepository
        );
    }

    @Test
    void should_return_last_reading_per_room_and_skip_rooms_that_never_reported() {
        final LocalDateTime fresh = NOW.minusHours(24);
        final LocalDateTime stale = NOW.minusHours(24).minusSeconds(1);

        when(temperatureRepository.findFirstByRoomOrderByDateDesc("office"))
            .thenReturn(Mono.just(new TemperatureEntity(1L, fresh, "office", 21.5)));
        when(temperatureRepository.findFirstByRoomOrderByDateDesc("garage"))
            .thenReturn(Mono.just(new TemperatureEntity(2L, stale, "garage", 12.0)));
        when(temperatureRepository.findFirstByRoomOrderByDateDesc("loft"))
            .thenReturn(Mono.empty());

        sut.querySensors()
            .as(StepVerifier::create)
            //exactly the limit is still fine, the sensor is stale only past it
            .expectNext(new TemperatureSensorReply(RoomName.OFFICE, fresh, false, false))
            //a muted room is still listed, with its real state
            .expectNext(new TemperatureSensorReply(RoomName.GARAGE, stale, true, true))
            .verifyComplete();
    }

    private static Room room(final RoomName name) {
        return Room.builder().name(name).build();
    }
}
