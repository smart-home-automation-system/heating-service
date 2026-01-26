package cloud.cholewa.heating.config;

import cloud.cholewa.heating.infrastructure.error.HeatingException;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.home.model.RoomName;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriBuilder;

import java.net.URI;

import static cloud.cholewa.heating.model.HeaterType.RADIATOR;

@Component
@RequiredArgsConstructor
public class ShellyConfig {

    @Value("${shelly.actor.scheme}")
    private String scheme;
    @Value("${shelly.actor.port}")
    private int port;
    @Value("${shelly.actor.pro4.down_left.host}")
    private String SHELLY_PRO4_DOWN_LEFT;
    @Value("${shelly.actor.pro4.down_right.host}")
    private String SHELLY_PRO4_DOWN_RIGHT;
    @Value("${shelly.actor.pro4.up_left.host}")
    private String SHELLY_PRO4_UP_LEFT;
    @Value("${shelly.actor.pro4.up_right.host}")
    private String SHELLY_PRO4_UP_RIGHT;

    private final RelayConfig relayConfig;

    public URI getStatusUri(final UriBuilder uriBuilder, final HeaterType heaterType, final RoomName roomName) {
        return uriBuilder
            .scheme(scheme)
            .host(heaterType == RADIATOR ? getShellyHeaterHost(roomName) : getShellyFloorHost(roomName))
            .port(port)
            .path("rpc/Switch.GetStatus")
            .queryParam("id", heaterType == RADIATOR ? relayConfig.getHeater(roomName) : relayConfig.getFloor(roomName))
            .build();
    }

    public URI getControlUri(
        final UriBuilder uriBuilder,
        final HeaterType heaterType,
        final RoomName roomName,
        final boolean enable
    ) {
        return uriBuilder
            .scheme(scheme)
            .host(heaterType == RADIATOR ? getShellyHeaterHost(roomName) : getShellyFloorHost(roomName))
            .port(port)
            .pathSegment("relay")
            .path(String.valueOf(heaterType == HeaterType.RADIATOR
                ? relayConfig.getHeater(roomName)
                : relayConfig.getFloor(roomName)))
            .queryParam("turn", enable ? "on" : "off")
            .build();
    }

    private String getShellyHeaterHost(final RoomName roomName) {
        return switch (roomName) {
            case OFFICE, TOBI, LIVIA, BEDROOM -> SHELLY_PRO4_UP_RIGHT;
            case BATHROOM_UP -> SHELLY_PRO4_UP_LEFT;
            case LIVING_ROOM, BATHROOM_DOWN -> SHELLY_PRO4_DOWN_LEFT;
            case CINEMA, ENTRANCE, GARAGE -> SHELLY_PRO4_DOWN_RIGHT;
            default -> throw new HeatingException("Unknown configuration for room heater: " + roomName.name());
        };
    }

    private String getShellyFloorHost(final RoomName roomName) {
        return switch (roomName) {
            case WARDROBE, BATHROOM_UP -> SHELLY_PRO4_UP_LEFT;
            case LIVING_ROOM, BATHROOM_DOWN -> SHELLY_PRO4_DOWN_LEFT;
            default -> throw new HeatingException("Unknown configuration for room floor: " + roomName.name());
        };
    }
}
