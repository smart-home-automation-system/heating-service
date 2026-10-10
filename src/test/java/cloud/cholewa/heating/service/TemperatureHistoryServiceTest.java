package cloud.cholewa.heating.service;

import cloud.cholewa.heating.db.model.TemperatureBucket;
import cloud.cholewa.heating.db.repository.TemperatureRepository;
import cloud.cholewa.heating.infrastructure.error.RoomNotFoundException;
import cloud.cholewa.heating.mapper.RoomMapper;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.Temperature;
import cloud.cholewa.heating.model.TemperatureHistoryReply;
import cloud.cholewa.home.model.RoomName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemperatureHistoryServiceTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 10, 8, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 10, 9, 0, 0);

    @Mock
    private TemperatureRepository temperatureRepository;

    private TemperatureHistoryService sut() {
        final Room livingRoom = Room.builder()
            .name(RoomName.LIVING_ROOM)
            .temperature(Temperature.builder().build())
            .build();

        return new TemperatureHistoryService(
            new RoomService(new Home(List.of(livingRoom)), new RoomMapper(Clock.systemDefaultZone())),
            temperatureRepository
        );
    }

    //the readings are stored under the name of the configuration: asked for as the caller wrote
    //the room, "Living Room" would find nothing and answer an empty history with a 200
    @Test
    void should_ask_for_the_readings_by_the_name_of_the_configuration() {
        when(temperatureRepository.findHistory("living room", FROM, TO, 1200)).thenReturn(Flux.just(
            new TemperatureBucket(LocalDateTime.of(2026, 10, 8, 0, 0), 21.44),
            new TemperatureBucket(LocalDateTime.of(2026, 10, 8, 0, 40), 21.38)
        ));

        sut().queryHistory("Living Room", FROM, TO)
            .as(StepVerifier::create)
            .expectNext(new TemperatureHistoryReply(
                RoomName.LIVING_ROOM, FROM, TO, 1200, List.of(
                new TemperatureHistoryReply.Point(LocalDateTime.of(2026, 10, 8, 0, 0), 21.44),
                new TemperatureHistoryReply.Point(LocalDateTime.of(2026, 10, 8, 0, 40), 21.38)
            )))
            .verifyComplete();
    }

    @Test
    void should_answer_a_range_without_readings_with_no_points() {
        when(temperatureRepository.findHistory("living room", FROM, TO, 1200)).thenReturn(Flux.empty());

        sut().queryHistory("living room", FROM, TO)
            .as(StepVerifier::create)
            .expectNext(new TemperatureHistoryReply(RoomName.LIVING_ROOM, FROM, TO, 1200, List.of()))
            .verifyComplete();
    }

    @Test
    void should_ask_for_wider_buckets_over_a_longer_range() {
        final LocalDateTime monthLater = FROM.plusDays(30);
        when(temperatureRepository.findHistory("living room", FROM, monthLater, 10800)).thenReturn(Flux.empty());

        sut().queryHistory("living room", FROM, monthLater)
            .as(StepVerifier::create)
            .assertNext(reply -> assertThat(reply.bucketSeconds()).isEqualTo(10800))
            .verifyComplete();
    }

    @Test
    void should_refuse_an_unknown_room_without_asking_the_database() {
        sut().queryHistory("attic", FROM, TO)
            .as(StepVerifier::create)
            .expectErrorSatisfies(throwable -> assertThat(throwable)
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessage("attic"))
            .verify();

        verifyNoInteractions(temperatureRepository);
    }

    //a range that is refused is refused for every room, known or not, and costs no query
    @Test
    void should_refuse_a_range_that_is_too_long_without_asking_the_database() {
        sut().queryHistory("living room", FROM, FROM.plusDays(32))
            .as(StepVerifier::create)
            .expectErrorSatisfies(throwable -> assertThat(throwable)
                .isInstanceOfSatisfying(ResponseStatusException.class, refusal ->
                    assertThat(refusal.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)))
            .verify();

        verifyNoInteractions(temperatureRepository);
    }
}
