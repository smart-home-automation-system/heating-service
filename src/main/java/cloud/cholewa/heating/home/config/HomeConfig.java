package cloud.cholewa.heating.home.config;

import cloud.cholewa.heating.model.BoilerRoom;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.HeatingSource;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.OpeningSensor;
import cloud.cholewa.heating.model.Pump;
import cloud.cholewa.heating.model.PumpType;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.TemperatureSensor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static cloud.cholewa.heating.model.HeaterType.FLOOR;
import static cloud.cholewa.heating.model.HeaterType.RADIATOR;
import static cloud.cholewa.heating.model.HeatingSourceType.FURNACE;
import static cloud.cholewa.heating.model.OpeningType.DOOR;
import static cloud.cholewa.heating.model.OpeningType.ENTRANCE_DOOR;
import static cloud.cholewa.heating.model.OpeningType.GATE_KATE;
import static cloud.cholewa.heating.model.OpeningType.GATE_KRIS;
import static cloud.cholewa.heating.model.OpeningType.ROOF;
import static cloud.cholewa.heating.model.OpeningType.ROOF2;
import static cloud.cholewa.heating.model.OpeningType.WINDOW;
import static cloud.cholewa.heating.model.OpeningType.WINDOW2;
import static cloud.cholewa.heating.model.PumpType.FIREPLACE_PUMP;
import static cloud.cholewa.heating.model.PumpType.HEATING_PUMP;
import static cloud.cholewa.heating.model.PumpType.HOT_WATER_PUMP;
import static cloud.cholewa.heating.model.RoomNames.BATHROOM_DOWN;
import static cloud.cholewa.heating.model.RoomNames.BATHROOM_UP;
import static cloud.cholewa.heating.model.RoomNames.BEDROOM;
import static cloud.cholewa.heating.model.RoomNames.CINEMA;
import static cloud.cholewa.heating.model.RoomNames.ENTRANCE;
import static cloud.cholewa.heating.model.RoomNames.GARAGE;
import static cloud.cholewa.heating.model.RoomNames.LIVIA;
import static cloud.cholewa.heating.model.RoomNames.MAIN;
import static cloud.cholewa.heating.model.RoomNames.OFFICE;
import static cloud.cholewa.heating.model.RoomNames.TOBI;
import static cloud.cholewa.heating.model.RoomNames.WARDROBE;

@Configuration
public class HomeConfig {

    @Bean
    public Home home() {
        return new Home(
            getRoomsConfiguration(),
            getBoilerConfiguration()
        );
    }

    private List<Room> getRoomsConfiguration() {
        return List.of(
            getOfficeConfiguration(),
            getTobiRoomConfiguration(),
            getLiviaRoomConfiguration(),
            getBedroomConfiguration(),
            getWardrobeConfiguration(),
            getBathroomUpConfiguration(),
            getMainConfiguration(),
            getCinemaConfiguration(),
            getBathroomDownConfiguration(),
            getEntranceConfiguration(),
            getGarageConfiguration()
        );
    }

    public BoilerRoom getBoilerConfiguration() {
        return BoilerRoom.builder()
            .heatingSources(List.of(
                HeatingSource.builder().type(FURNACE).build()
            ))
            .pumps(List.of(
                Pump.builder().type(HEATING_PUMP).build(),
                Pump.builder().type(FIREPLACE_PUMP).build(),
                Pump.builder().type(HOT_WATER_PUMP).build(),
                Pump.builder().type(PumpType.FLOOR_PUMP).build()
            ))
            .build();
    }

    private Room getOfficeConfiguration() {
        return Room.builder()
            .name(OFFICE)
            .temperatureSensor(TemperatureSensor.builder().build())
            .openingSensors(List.of(
                OpeningSensor.builder().openingType(DOOR).build(),
                OpeningSensor.builder().openingType(WINDOW).build()
            ))
            .heaterActors(List.of(
                HeaterActor.builder().type(RADIATOR).build()
            ))
            .build();
    }

    private Room getTobiRoomConfiguration() {
        return Room.builder()
            .name(TOBI)
            .temperatureSensor(TemperatureSensor.builder().build())
            .openingSensors(List.of(
                OpeningSensor.builder().openingType(DOOR).build(),
                OpeningSensor.builder().openingType(WINDOW).build()
            ))
            .heaterActors(List.of(
                HeaterActor.builder().type(RADIATOR).build()
            ))
            .build();
    }

    private Room getLiviaRoomConfiguration() {
        return Room.builder()
            .name(LIVIA)
            .temperatureSensor(TemperatureSensor.builder().build())
            .openingSensors(List.of(
                OpeningSensor.builder().openingType(WINDOW).build(),
                OpeningSensor.builder().openingType(ROOF).build()
            ))
            .heaterActors(List.of(
                HeaterActor.builder().type(RADIATOR).build()
            ))
            .build();
    }

    private Room getBedroomConfiguration() {
        return Room.builder()
            .name(BEDROOM)
            .temperatureSensor(TemperatureSensor.builder().build())
            .openingSensors(List.of(
                OpeningSensor.builder().openingType(WINDOW).build(),
                OpeningSensor.builder().openingType(WINDOW2).build(),
                OpeningSensor.builder().openingType(DOOR).build()
            ))
            .heaterActors(List.of(
                HeaterActor.builder().type(RADIATOR).build()
            ))
            .build();
    }

    private Room getWardrobeConfiguration() {
        return Room.builder()
            .name(WARDROBE)
            .temperatureSensor(TemperatureSensor.builder().build())
            .openingSensors(List.of(
                OpeningSensor.builder().openingType(ROOF).build(),
                OpeningSensor.builder().openingType(ROOF2).build()
            ))
            .heaterActors(List.of(
                HeaterActor.builder().type(FLOOR).build()
            ))
            .build();
    }

    private Room getBathroomUpConfiguration() {
        return Room.builder()
            .name(BATHROOM_UP)
            .temperatureSensor(TemperatureSensor.builder().build())
            .openingSensors(List.of(
                OpeningSensor.builder().openingType(ROOF).build()
            ))
            .heaterActors(List.of(
                HeaterActor.builder().type(FLOOR).build(),
                HeaterActor.builder().type(RADIATOR).build()
            ))
            .build();
    }

    private Room getMainConfiguration() {
        return Room.builder()
            .name(MAIN)
            .temperatureSensor(TemperatureSensor.builder().build())
            .openingSensors(List.of(
                OpeningSensor.builder().openingType(DOOR).build(),
                OpeningSensor.builder().openingType(WINDOW).build(),
                OpeningSensor.builder().openingType(WINDOW2).build(),
                OpeningSensor.builder().openingType(ENTRANCE_DOOR).build()
            ))
            .heaterActors(List.of(
                HeaterActor.builder().type(FLOOR).build(),
                HeaterActor.builder().type(RADIATOR).build()
            ))
            .build();
    }

    private Room getBathroomDownConfiguration() {
        return Room.builder()
            .name(BATHROOM_DOWN)
            .temperatureSensor(TemperatureSensor.builder().build())
            .heaterActors(List.of(
                HeaterActor.builder().type(FLOOR).build(),
                HeaterActor.builder().type(RADIATOR).build()
            ))
            .build();
    }

    private Room getCinemaConfiguration() {
        return Room.builder()
            .name(CINEMA)
            .temperatureSensor(TemperatureSensor.builder().build())
            .openingSensors(List.of(
                OpeningSensor.builder().openingType(WINDOW).build(),
                OpeningSensor.builder().openingType(WINDOW2).build()
            ))
            .heaterActors(List.of(
                HeaterActor.builder().type(RADIATOR).build()
            ))
            .build();
    }

    private Room getEntranceConfiguration() {
        return Room.builder()
            .name(ENTRANCE)
            .temperatureSensor(TemperatureSensor.builder().build())
            .openingSensors(List.of(
                OpeningSensor.builder().openingType(ENTRANCE_DOOR).build()
            ))
            .heaterActors(List.of(
                HeaterActor.builder().type(RADIATOR).build()
            ))
            .build();
    }

    private Room getGarageConfiguration() {
        return Room.builder()
            .name(GARAGE)
            .temperatureSensor(TemperatureSensor.builder().build())
            .openingSensors(List.of(
                OpeningSensor.builder().openingType(GATE_KATE).build(),
                OpeningSensor.builder().openingType(GATE_KRIS).build()
            ))
            .heaterActors(List.of(
                HeaterActor.builder().type(RADIATOR).build()
            ))
            .build();
    }
}
