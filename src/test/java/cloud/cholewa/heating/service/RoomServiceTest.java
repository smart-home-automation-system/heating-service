package cloud.cholewa.heating.service;

import cloud.cholewa.heating.infrastructure.error.RoomNotFoundException;
import cloud.cholewa.heating.mapper.RoomMapper;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.RoomReply;
import cloud.cholewa.heating.model.Temperature;
import cloud.cholewa.home.model.RoomName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoomServiceTest {

    private final Room office = Room.builder()
        .name(RoomName.OFFICE)
        .temperature(Temperature.builder().build())
        .build();
    private final Room livingRoom = Room.builder()
        .name(RoomName.LIVING_ROOM)
        .temperature(Temperature.builder().build())
        .build();

    private final RoomService sut = new RoomService(new Home(List.of(office, livingRoom)), new RoomMapper(Clock.systemDefaultZone()));

    @Test
    void should_list_the_rooms_in_the_order_of_the_configuration() {
        sut.queryRooms()
            .map(RoomReply::name)
            .as(StepVerifier::create)
            .expectNext(RoomName.OFFICE, RoomName.LIVING_ROOM)
            .verifyComplete();
    }

    @Test
    void should_list_a_room_without_readings_without_a_temperature() {
        sut.queryRooms()
            .as(StepVerifier::create)
            .assertNext(room -> assertThat(room.temperature()).isNull())
            .assertNext(room -> assertThat(room.temperature()).isNull())
            .verifyComplete();
    }

    //the state is read at subscription: a reply built once and subscribed to again is not a stale copy
    @Test
    void should_read_the_state_when_subscribed_to() {
        final Flux<RoomReply> rooms = sut.queryRooms();
        final Mono<RoomReply> room = sut.queryRoom("office");
        final LocalDateTime readingAt = LocalDateTime.of(2026, 10, 8, 18, 30);

        office.getTemperature().setValue(21.3);
        office.getTemperature().setUpdatedAt(readingAt);

        rooms.as(StepVerifier::create)
            .assertNext(reply -> assertThat(reply.temperature()).isEqualTo(new RoomReply.Reading(21.3, readingAt)))
            .expectNextCount(1)
            .verifyComplete();

        room.as(StepVerifier::create)
            .assertNext(reply -> assertThat(reply.temperature()).isEqualTo(new RoomReply.Reading(21.3, readingAt)))
            .verifyComplete();
    }

    @ParameterizedTest
    @ValueSource(strings = {"living room", "Living Room", "LIVING ROOM"})
    void should_find_a_room_by_the_name_of_the_list_in_any_case(final String name) {
        sut.queryRoom(name)
            .as(StepVerifier::create)
            .assertNext(room -> assertThat(room.name()).isEqualTo(RoomName.LIVING_ROOM))
            .verifyComplete();
    }

    //the readings of a room are stored under this name, whatever the caller wrote
    @ParameterizedTest
    @ValueSource(strings = {"living room", "Living Room", "LIVING ROOM"})
    void should_name_a_room_as_the_configuration_does(final String name) {
        sut.queryRoomName(name)
            .as(StepVerifier::create)
            .expectNext(RoomName.LIVING_ROOM)
            .verifyComplete();
    }

    @Test
    void should_refuse_to_name_an_unknown_room() {
        sut.queryRoomName("attic")
            .as(StepVerifier::create)
            .expectErrorSatisfies(throwable -> assertThat(throwable)
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessage("attic"))
            .verify();
    }

    //"attic" is no room at all, "kitchen" a RoomName the house has no room for, "LIVING_ROOM" the
    //name of the constant - only the name the list gives a room finds it
    @ParameterizedTest
    @ValueSource(strings = {"attic", "kitchen", "LIVING_ROOM", "living", " "})
    void should_refuse_an_unknown_room(final String name) {
        sut.queryRoom(name)
            .as(StepVerifier::create)
            .expectErrorSatisfies(throwable -> assertThat(throwable)
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessage(name))
            .verify();
    }
}
