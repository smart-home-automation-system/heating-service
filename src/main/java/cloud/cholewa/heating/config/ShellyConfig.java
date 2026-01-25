package cloud.cholewa.heating.config;

import cloud.cholewa.heating.infrastructure.error.HeatingException;
import cloud.cholewa.home.model.RoomName;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class ShellyConfig {

    @Value("${shelly.actor.pro4.down_left.host}")
    private String SHELLY_PRO4_DOWN_LEFT;
    @Value("${shelly.actor.pro4.down_right.host}")
    private String SHELLY_PRO4_DOWN_RIGHT;
    @Value("${shelly.actor.pro4.up_left.host}")
    private String SHELLY_PRO4_UP_LEFT;
    @Value("${shelly.actor.pro4.up_right.host}")
    private String SHELLY_PRO4_UP_RIGHT;

    public HeaterPro4Data getShellyPro4HeaterHost(final RoomName roomName) {
        return switch (roomName) {
            case OFFICE -> new HeaterPro4Data(SHELLY_PRO4_UP_RIGHT, "3");
            case TOBI -> new HeaterPro4Data(SHELLY_PRO4_UP_RIGHT, "2");
            case LIVIA -> new HeaterPro4Data(SHELLY_PRO4_UP_RIGHT, "1");
            case BEDROOM -> new HeaterPro4Data(SHELLY_PRO4_UP_RIGHT, "0");
            case BATHROOM_UP -> new HeaterPro4Data(SHELLY_PRO4_UP_LEFT, "3");
            case LIVING_ROOM -> new HeaterPro4Data(SHELLY_PRO4_DOWN_LEFT, "1");
            case CINEMA -> new HeaterPro4Data(SHELLY_PRO4_DOWN_RIGHT, "1");
            case BATHROOM_DOWN -> new HeaterPro4Data(SHELLY_PRO4_DOWN_LEFT, "3");
            case ENTRANCE -> new HeaterPro4Data(SHELLY_PRO4_DOWN_RIGHT, "0");
            case GARAGE -> new HeaterPro4Data(SHELLY_PRO4_DOWN_RIGHT, "2");
            default -> throw new HeatingException("Unknown configuration for room heater: " + roomName.name());
        };
    }

    public HeaterPro4Data getShellyPro4FloorHost(final RoomName roomName) {
        return switch (roomName) {
            case WARDROBE -> new HeaterPro4Data(SHELLY_PRO4_UP_LEFT, "2");
            case BATHROOM_UP -> new HeaterPro4Data(SHELLY_PRO4_UP_LEFT, "1");
            case LIVING_ROOM -> new HeaterPro4Data(SHELLY_PRO4_DOWN_LEFT, "0");
            case BATHROOM_DOWN -> new HeaterPro4Data(SHELLY_PRO4_DOWN_LEFT, "2");
            default -> throw new HeatingException("Unknown configuration for room floor: " + roomName.name());
        };
    }

    public record HeaterPro4Data(String host, String relay) {}
}
