package cloud.cholewa.heating.config;

import cloud.cholewa.heating.infrastructure.error.HeatingException;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.home.model.RoomName;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Setter
@Component
@ConfigurationProperties(prefix = "shelly.actor.relay")
public class RelayConfig {
    private Map<String, Integer> heater = new HashMap<>();
    private Map<String, Integer> floor = new HashMap<>();

    public int getHeater(final RoomName roomName) {
        return heater.entrySet().stream()
            .filter(entry -> entry.getKey().replace("_", " ").equalsIgnoreCase(roomName.getValue()))
            .findFirst()
            .map(Map.Entry::getValue)
            .orElseThrow(() -> new HeatingException("Unknown configuration for room heater: " + roomName));
    }

    //whether this service drives that heater - SANCTUM's radiator is switched outside it (HAS-355: no
    //schedule is loaded for a heater it does not drive)
    public boolean drives(final RoomName roomName, final HeaterType type) {
        final Map<String, Integer> relays = type == HeaterType.FLOOR ? floor : heater;
        return relays.keySet().stream()
            .anyMatch(key -> key.replace("_", " ").equalsIgnoreCase(roomName.getValue()));
    }

    public int getFloor(final RoomName roomName) {
        return floor.entrySet().stream()
            .filter(entry -> entry.getKey().replace("_", " ").equalsIgnoreCase(roomName.getValue()))
            .findFirst()
            .map(Map.Entry::getValue)
            .orElseThrow(() -> new HeatingException("Unknown configuration for room floor: " + roomName));
    }
}
