package cloud.cholewa.heating.config;

import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Humidity;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.RoomMode;
import cloud.cholewa.heating.model.Temperature;
import cloud.cholewa.home.model.RoomName;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

//The rooms and their heaters. The schedules of the heaters are stored in heating_schedule since HAS-355
//(HeatingScheduleService) - a change of an hour is no longer a release
@Configuration
public class HomeConfig {

    @Bean
    Home home(final List<Room> rooms) {
        return new Home(rooms);
    }

    @Bean
    List<Room> rooms(
        final Room office,
        final Room tobi,
        final Room livia,
        final Room bedroom,
        final Room wardrobe,
        final Room bathroomUp,
        final Room loft,
        final Room livingRoom,
        final Room cinema,
        final Room bathroomDown,
        final Room entrance,
        final Room garage,
        final Room sanctum,
        final Room sauna,
        final Room garden
    ) {
        return List.of(
            office, tobi, livia, bedroom, wardrobe, bathroomUp, loft,
            livingRoom, cinema, bathroomDown, entrance, garage, sanctum, sauna, garden
        );
    }

    @Bean
    Room office() {
        return Room.builder()
            .name(RoomName.OFFICE)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR).build())
            .build();
    }

    @Bean
    Room tobi() {
        return Room.builder()
            .name(RoomName.TOBI)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR).build())
            .build();
    }

    @Bean
    Room livia() {
        return Room.builder()
            .name(RoomName.LIVIA)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR).build())
            .build();
    }

    @Bean
    Room bedroom() {
        return Room.builder()
            .name(RoomName.BEDROOM)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR).build())
            .build();
    }

    @Bean
    Room wardrobe() {
        return Room.builder()
            .name(RoomName.WARDROBE)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.FLOOR).build())
            .build();
    }

    @Bean
    Room bathroomUp() {
        return Room.builder()
            .name(RoomName.BATHROOM_UP)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR).build())
            .heaterActor(HeaterActor.builder().type(HeaterType.FLOOR).build())
            .build();
    }

    @Bean
    Room loft() {
        return Room.builder()
            .name(RoomName.LOFT)
            .temperature(Temperature.builder().build())
            .build();
    }

    @Bean
    Room livingRoom() {
        return Room.builder()
            .name(RoomName.LIVING_ROOM)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR).build())
            .heaterActor(HeaterActor.builder().type(HeaterType.FLOOR).build())
            .build();
    }

    @Bean
    Room cinema() {
        return Room.builder()
            .name(RoomName.CINEMA)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR).build())
            .build();
    }

    @Bean
    Room bathroomDown() {
        return Room.builder()
            .name(RoomName.BATHROOM_DOWN)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR).build())
            .heaterActor(HeaterActor.builder().type(HeaterType.FLOOR).build())
            .build();
    }

    @Bean
    Room entrance() {
        return Room.builder()
            .name(RoomName.ENTRANCE)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR).build())
            .build();
    }

    @Bean
    Room garage() {
        return Room.builder()
            .name(RoomName.GARAGE)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR).build())
            .build();
    }

    @Bean
    Room sanctum() {
        return Room.builder()
            .name(RoomName.SANCTUM)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR).build())
            .build();
    }

    @Bean
    Room sauna() {
        return Room.builder()
            .name(RoomName.SAUNA)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .build();
    }

    @Bean
    Room garden() {
        return Room.builder()
            .name(RoomName.GARDEN)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .build();
    }
}
