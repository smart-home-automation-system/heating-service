package cloud.cholewa.heating.config.home;

import cloud.cholewa.heating.model.BoilerRoom;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.HeatingSource;
import cloud.cholewa.heating.model.OpeningSensor;
import cloud.cholewa.heating.model.Pump;
import cloud.cholewa.heating.model.PumpType;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.TemperatureSensor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

import static cloud.cholewa.heating.model.HeaterType.FLOOR;
import static cloud.cholewa.heating.model.HeaterType.RADIATOR;
import static cloud.cholewa.heating.model.HeatingSourceType.FURNACE;
import static cloud.cholewa.heating.model.OpeningType.DOOR;
import static cloud.cholewa.heating.model.OpeningType.ROOF;
import static cloud.cholewa.heating.model.OpeningType.ROOF2;
import static cloud.cholewa.heating.model.OpeningType.WINDOW;
import static cloud.cholewa.heating.model.OpeningType.WINDOW2;
import static cloud.cholewa.heating.model.PumpType.FIREPLACE_PUMP;
import static cloud.cholewa.heating.model.PumpType.HEATING_PUMP;
import static cloud.cholewa.heating.model.PumpType.HOT_WATER_PUMP;
import static cloud.cholewa.heating.model.RoomNames.BATHROOM_UP;
import static cloud.cholewa.heating.model.RoomNames.BEDROOM;
import static cloud.cholewa.heating.model.RoomNames.LIVIA;
import static cloud.cholewa.heating.model.RoomNames.OFFICE;
import static cloud.cholewa.heating.model.RoomNames.TOBI;
import static cloud.cholewa.heating.model.RoomNames.WARDROBE;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HomeConfig {

    public static List<Room> getRoomsConfiguration() {
        return List.of(
            getOfficeConfiguration(),
            getTobiRoomConfiguration(),
            getLiviaRoomConfiguration(),
            getBedroomConfiguration(),
            getWardrobeConfiguration(),
            getBathroomUpConfiguration()
        );
    }

    public static BoilerRoom getBoilerRoomConfiguration() {
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

    private static Room getOfficeConfiguration() {
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

    private static Room getTobiRoomConfiguration() {
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

    private static Room getLiviaRoomConfiguration() {
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

    private static Room getBedroomConfiguration() {
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

    private static Room getWardrobeConfiguration() {
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

    private static Room getBathroomUpConfiguration() {
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
}
