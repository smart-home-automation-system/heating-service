package cloud.cholewa.heating.home.service;

import cloud.cholewa.heating.error.RoomNotFoundException;
import cloud.cholewa.heating.model.BoilerRoom;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.RoomNames;
import cloud.cholewa.heating.model.TemperatureSensor;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

class HomeServiceTest {

    List<Room> rooms = List.of(
        Room.builder().name(RoomNames.CINEMA).temperatureSensor(TemperatureSensor.builder().build()).build(),
        Room.builder().name(RoomNames.GARAGE).temperatureSensor(TemperatureSensor.builder().build()).build()
    );

    BoilerRoom boilerRoom = BoilerRoom.builder().build();

    private final Home home = new Home(rooms, boilerRoom);

    private final HomeService sut = new HomeService(home);

    @Test
    void should_return_home_configuration() {
        sut.getHomeConfiguration()
            .as(StepVerifier::create)
            .expectNextMatches(home -> home.getRoomNumber() == 2)
            .verifyComplete();
    }

    @Test
    void should_return_room_configuration_when_room_exists() {

        sut.getRoomConfiguration("garage")
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void should_return_empty_configuration_when_room_not_exists() {

        sut.getRoomConfiguration("dummy")
            .as(StepVerifier::create)
            .expectError(RoomNotFoundException.class)
            .verify();
    }
}