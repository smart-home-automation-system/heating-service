package cloud.cholewa.heating.home.service;

import cloud.cholewa.heating.model.Alert;
import cloud.cholewa.heating.model.BoilerRoom;
import cloud.cholewa.heating.model.Fireplace;
import cloud.cholewa.heating.model.Furnace;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.HotWater;
import cloud.cholewa.heating.model.Pump;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.Temperature;
import cloud.cholewa.heating.model.WaterCirculation;
import cloud.cholewa.home.model.RoomName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

class HomeServiceTest {

    private final List<Pump> pumps = List.of(Pump.builder().build());
    private final WaterCirculation circulation = new WaterCirculation(new Temperature(), Pump.builder().build());
    private final HotWater hotWater = new HotWater(new Temperature(), circulation);
    private final Fireplace fireplace = new Fireplace(new Temperature());
    private final Furnace furnace = new Furnace();
    private final Alert alert = Alert.builder().build();
    private final BoilerRoom boilerRoom = new BoilerRoom(alert, furnace, fireplace, hotWater, pumps);
    private final List<Room> rooms = List.of(Room.builder().name(RoomName.OFFICE).build());
    private final Home home = new Home(boilerRoom, rooms);
    private final HomeService sut = new HomeService(home);

    @Test
    void should_return_room_when_room_name_valid() {
        sut.getRoomStatusByName("office")
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void should_return_error_room_name_not_valid() {
        sut.getRoomStatusByName("garage")
            .as(StepVerifier::create)
            .verifyError();
    }
}