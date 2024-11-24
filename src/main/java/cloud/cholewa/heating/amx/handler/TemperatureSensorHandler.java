package cloud.cholewa.heating.amx.handler;

import cloud.cholewa.heating.model.Fireplace;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.home.model.DeviceStatusUpdate;
import cloud.cholewa.home.model.RoomName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TemperatureSensorHandler {

    private final Fireplace fireplace;

    private final Room office;
    private final Room tobi;
    private final Room livia;
    private final Room bedroom;
    private final Room wardrobe;
    private final Room bathroomUp;
    private final Room loft;
    private final Room livingRoom;
    private final Room cinema;
    private final Room bathroomDown;
    private final Room entrance;
    private final Room garage;
    private final Room sanctum;
    private final Room sauna;
    private final Room garden;

    public Mono<Void> handle(final DeviceStatusUpdate device) {
        switch (device.getRoomName()) {
            case OFFICE -> handleRoom(office, device);
            case TOBI -> handleRoom(tobi, device);
            case LIVIA -> handleRoom(livia, device);
            case BEDROOM -> handleRoom(bedroom, device);
            case WARDROBE -> handleRoom(wardrobe, device);
            case BATHROOM_UP -> handleRoom(bathroomUp, device);
            case LOFT -> handleRoom(loft, device);
            case LIVING_ROOM -> handleRoom(livingRoom, device);
            case CINEMA -> handleRoom(cinema, device);
            case BATHROOM_DOWN -> handleRoom(bathroomDown, device);
            case ENTRANCE -> handleRoom(entrance, device);
            case GARAGE -> handleRoom(garage, device);
            case SANCTUM -> handleRoom(sanctum, device);
            case SAUNA -> handleRoom(sauna, device);
            case GARDEN -> handleRoom(garden, device);
            case BOILER -> handleBoiler(device);

            default -> log.error("No handler for room: [{}]", device.getRoomName().name());
        }

        return Mono.empty();
    }

    private void handleBoiler(final DeviceStatusUpdate device) {
        fireplace.temperature().setUpdatedAt(LocalDateTime.now());
        fireplace.temperature().setValue(Double.parseDouble(device.getValue()));
    }

    private void handleRoom(final Room room, final DeviceStatusUpdate device) {
        room.getTemperature().setUpdatedAt(LocalDateTime.now());

        if (room.getName().equalsIgnoreCase(RoomName.BATHROOM_DOWN.name())) {
            //This is necessary due to failure of bathroom down sensor
            room.getTemperature().setValue(Double.parseDouble(device.getValue()) + 3);
        } else {
            room.getTemperature().setValue(Double.parseDouble(device.getValue()));
        }
    }
}
