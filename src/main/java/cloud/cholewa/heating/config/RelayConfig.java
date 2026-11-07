package cloud.cholewa.heating.config;

import cloud.cholewa.heating.infrastructure.error.HeatingException;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.home.model.RoomName;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Setter
@Component
@ConfigurationProperties(prefix = "shelly.actor.relay")
public class RelayConfig {
    private Map<String, Integer> heater = new HashMap<>();
    private Map<String, Integer> floor = new HashMap<>();
    //the radiators that are a Shelly device of their own, not a relay of a Pro 4 (HAS-361): the address of each by its
    //room - the Plug S Gen1 of sanctum. Driven like every heater, but never part of what the furnace hears
    private Map<String, String> standalone = new HashMap<>();
    //the radiators whose schedules are kept and served though this service switches nothing for them (HAS-388): the
    //radiator of sanctum, heated by its air conditioner since - aircon-service reads the schedule of the room
    private List<String> scheduleOnly = new ArrayList<>();

    public int getHeater(final RoomName roomName) {
        return heater.entrySet().stream()
            .filter(entry -> entry.getKey().replace("_", " ").equalsIgnoreCase(roomName.getValue()))
            .findFirst()
            .map(Map.Entry::getValue)
            .orElseThrow(() -> new HeatingException("Unknown configuration for room heater: " + roomName));
    }

    //whether this service drives that heater: a relay of a Pro 4 or a standalone radiator (HAS-361; no schedule is
    //loaded for a heater it neither drives nor keeps the schedules of, HAS-355 / HAS-388)
    public boolean drives(final RoomName roomName, final HeaterType type) {
        return drivesRelay(roomName, type) || isStandalone(roomName, type);
    }

    //whether a heater keeps its schedules: one this service drives, or a radiator left to another service (HAS-388) - its
    //schedules are stored, loaded, written through the API and answered in GET /rooms, and never switch anything here
    public boolean keepsSchedules(final RoomName roomName, final HeaterType type) {
        return drives(roomName, type)
            || type == HeaterType.RADIATOR && scheduleOnly.stream().anyMatch(room -> matches(room, roomName));
    }

    //a relay of a Pro 4 - what the furnace hears (boiler-service): never a standalone radiator, nor one left to another
    //service
    public boolean drivesRelay(final RoomName roomName, final HeaterType type) {
        final Map<String, Integer> relays = type == HeaterType.FLOOR ? floor : heater;
        return relays.keySet().stream().anyMatch(key -> matches(key, roomName));
    }

    //a radiator that is a Shelly device of its own (HAS-361)
    public boolean isStandalone(final RoomName roomName, final HeaterType type) {
        return type == HeaterType.RADIATOR && standaloneAddress(roomName).isPresent();
    }

    //the address of the heater of this type when it is a standalone radiator, "host" or "host:port"
    public Optional<String> standaloneAddress(final RoomName roomName, final HeaterType type) {
        return Optional.of(type).filter(HeaterType.RADIATOR::equals).flatMap(radiator -> standaloneAddress(roomName));
    }

    private Optional<String> standaloneAddress(final RoomName roomName) {
        return standalone.entrySet().stream()
            .filter(entry -> matches(entry.getKey(), roomName))
            .findFirst()
            .map(Map.Entry::getValue);
    }

    private static boolean matches(final String key, final RoomName roomName) {
        return key.replace("_", " ").equalsIgnoreCase(roomName.getValue());
    }

    public int getFloor(final RoomName roomName) {
        return floor.entrySet().stream()
            .filter(entry -> entry.getKey().replace("_", " ").equalsIgnoreCase(roomName.getValue()))
            .findFirst()
            .map(Map.Entry::getValue)
            .orElseThrow(() -> new HeatingException("Unknown configuration for room floor: " + roomName));
    }
}
