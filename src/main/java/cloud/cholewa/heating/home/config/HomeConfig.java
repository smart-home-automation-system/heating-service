package cloud.cholewa.heating.home.config;

import cloud.cholewa.heating.model.BoilerRoom;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Humidity;
import cloud.cholewa.heating.model.OpeningSensor;
import cloud.cholewa.heating.model.OpeningType;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.RoomMode;
import cloud.cholewa.heating.model.Temperature;
import cloud.cholewa.home.model.RoomName;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class HomeConfig {

    @Bean
    Home home(final BoilerRoom boilerRoom, final List<Room> rooms
    ) {
        return new Home(boilerRoom, rooms);
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
        return List.of(office, tobi, livia, bedroom, wardrobe, bathroomUp, loft,
            livingRoom, cinema, bathroomDown, entrance, garage, sanctum, sauna, garden);
    }

    @Bean
    Room office() {
        return Room.builder()
            .name(RoomName.OFFICE)
            .mode(RoomMode.HEATING)
            .temperature(new Temperature())
            .humidity(new Humidity())
            .heaterActors(List.of(
                HeaterActor.builder().name(HeaterType.RADIATOR).build()
            ))
            .openingSensors(List.of(
                OpeningSensor.builder().name(OpeningType.DOOR).build(),
                OpeningSensor.builder().name(OpeningType.WINDOW).build()
            ))
            .build();
    }

    @Bean
    Room tobi() {
        return Room.builder()
            .name(RoomName.TOBI)
            .mode(RoomMode.HEATING)
            .temperature(new Temperature())
            .humidity(new Humidity())
            .heaterActors(List.of(
                HeaterActor.builder().name(HeaterType.RADIATOR).build()
            ))
            .openingSensors(List.of(
                OpeningSensor.builder().name(OpeningType.DOOR).build(),
                OpeningSensor.builder().name(OpeningType.WINDOW).build()
            ))
            .build();
    }

    @Bean
    Room livia() {
        return Room.builder()
            .name(RoomName.LIVIA)
            .mode(RoomMode.HEATING)
            .temperature(new Temperature())
            .humidity(new Humidity())
            .heaterActors(List.of(
                HeaterActor.builder().name(HeaterType.RADIATOR).build()
            ))
            .openingSensors(List.of(
                OpeningSensor.builder().name(OpeningType.ROOF).build(),
                OpeningSensor.builder().name(OpeningType.WINDOW).build()
            ))
            .build();
    }

    @Bean
    Room bedroom() {
        return Room.builder()
            .name(RoomName.BEDROOM)
            .mode(RoomMode.HEATING)
            .temperature(new Temperature())
            .humidity(new Humidity())
            .heaterActors(List.of(
                HeaterActor.builder().name(HeaterType.RADIATOR).build()
            ))
            .openingSensors(List.of(
                OpeningSensor.builder().name(OpeningType.WINDOW).build(),
                OpeningSensor.builder().name(OpeningType.DOOR).build(),
                OpeningSensor.builder().name(OpeningType.WINDOW2).build()
            ))
            .build();
    }

    @Bean
    Room wardrobe() {
        return Room.builder()
            .name(RoomName.WARDROBE)
            .mode(RoomMode.HEATING)
            .temperature(new Temperature())
            .humidity(new Humidity())
            .heaterActors(List.of(
                HeaterActor.builder().name(HeaterType.FLOOR).build()
            ))
            .openingSensors(List.of(
                OpeningSensor.builder().name(OpeningType.ROOF_LEFT).build(),
                OpeningSensor.builder().name(OpeningType.ROOF_RIGHT).build()
            ))
            .build();
    }

    @Bean
    Room bathroomUp() {
        return Room.builder()
            .name(RoomName.BATHROOM_UP)
            .mode(RoomMode.HEATING)
            .temperature(new Temperature())
            .humidity(new Humidity())
            .heaterActors(List.of(
                HeaterActor.builder().name(HeaterType.RADIATOR).build(),
                HeaterActor.builder().name(HeaterType.FLOOR).build()
            ))
            .openingSensors(List.of(
                OpeningSensor.builder().name(OpeningType.ROOF).build()
            ))
            .build();
    }

    @Bean
    Room loft() {
        return Room.builder()
            .name(RoomName.LOFT)
            .temperature(new Temperature())
            .build();
    }

    @Bean
    Room livingRoom() {
        return Room.builder()
            .name(RoomName.LIVING_ROOM)
            .mode(RoomMode.HEATING)
            .temperature(new Temperature())
            .humidity(new Humidity())
            .heaterActors(List.of(
                HeaterActor.builder().name(HeaterType.RADIATOR).build(),
                HeaterActor.builder().name(HeaterType.FLOOR).build()
            ))
            .openingSensors(List.of(
                OpeningSensor.builder().name(OpeningType.DOOR).build(),
                OpeningSensor.builder().name(OpeningType.ENTRANCE_DOOR).build()
            ))
            .build();
    }

    @Bean
    Room cinema() {
        return Room.builder()
            .name(RoomName.CINEMA)
            .mode(RoomMode.HEATING)
            .temperature(new Temperature())
            .humidity(new Humidity())
            .heaterActors(List.of(
                HeaterActor.builder().name(HeaterType.RADIATOR).build()
            ))
            .openingSensors(List.of(
                OpeningSensor.builder().name(OpeningType.WINDOW).build(),
                OpeningSensor.builder().name(OpeningType.WINDOW2).build()
            ))
            .build();
    }

    @Bean
    Room bathroomDown() {
        return Room.builder()
            .name(RoomName.BATHROOM_DOWN)
            .mode(RoomMode.HEATING)
            .temperature(new Temperature())
            .humidity(new Humidity())
            .heaterActors(List.of(
                HeaterActor.builder().name(HeaterType.RADIATOR).build(),
                HeaterActor.builder().name(HeaterType.FLOOR).build()
            ))
            .build();
    }

    @Bean
    Room entrance() {
        return Room.builder()
            .name(RoomName.ENTRANCE)
            .mode(RoomMode.HEATING)
            .temperature(new Temperature())
            .humidity(new Humidity())
            .heaterActors(List.of(
                HeaterActor.builder().name(HeaterType.RADIATOR).build()
            ))
            .openingSensors(List.of(
                OpeningSensor.builder().name(OpeningType.ENTRANCE_DOOR).build()
            ))
            .build();
    }

    @Bean
    Room garage() {
        return Room.builder()
            .name(RoomName.GARAGE)
            .mode(RoomMode.HEATING)
            .temperature(new Temperature())
            .humidity(new Humidity())
            .heaterActors(List.of(
                HeaterActor.builder().name(HeaterType.RADIATOR).build()
            ))
            .openingSensors(List.of(
                OpeningSensor.builder().name(OpeningType.GATE_KATE).build(),
                OpeningSensor.builder().name(OpeningType.GATE_KRIS).build()
            ))
            .build();
    }

    @Bean
    Room sanctum() {
        return Room.builder()
            .name(RoomName.SANCTUM)
            .mode(RoomMode.HEATING)
            .temperature(new Temperature())
            .humidity(new Humidity())
            .heaterActors(List.of(
                HeaterActor.builder().name(HeaterType.RADIATOR).build()
            ))
            .openingSensors(List.of(
                OpeningSensor.builder().name(OpeningType.DOOR).build()
            ))
            .build();
    }

    @Bean
    Room sauna() {
        return Room.builder()
            .name(RoomName.SAUNA)
            .temperature(new Temperature())
            .humidity(new Humidity())
            .build();
    }

    @Bean
    Room garden() {
        return Room.builder()
            .name(RoomName.GARDEN)
            .temperature(new Temperature())
            .humidity(new Humidity())
            .build();
    }
}
