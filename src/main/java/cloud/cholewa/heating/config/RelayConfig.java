package cloud.cholewa.heating.config;

import cloud.cholewa.heating.infrastructure.error.HeatingException;
import cloud.cholewa.home.model.RoomName;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Setter
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

    public int getFloor(final RoomName roomName) {
        return floor.entrySet().stream()
            .filter(entry -> entry.getKey().replace("_", " ").equalsIgnoreCase(roomName.getValue()))
            .findFirst()
            .map(Map.Entry::getValue)
            .orElseThrow(() -> new HeatingException("Unknown configuration for room floor: " + roomName));
    }
}
