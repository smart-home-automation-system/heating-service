package cloud.cholewa.heating.home.service;

import cloud.cholewa.heating.home.RoomMapper;
import cloud.cholewa.heating.home.model.HomeConfigurationResponse;
import cloud.cholewa.heating.home.model.RoomConfigurationResponse;
import cloud.cholewa.heating.model.Home;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomeService {

    private final Home home;

    public Mono<RoomConfigurationResponse> getRoomConfiguration(final String roomName) {
        log.info("Requesting of room configuration for room {}", roomName);

        return Mono.justOrEmpty(
                home.getRooms().stream()
                    .filter(room -> room.getName().name().equalsIgnoreCase(roomName))
                    .findAny()
            )
            .map(RoomMapper::map);
    }

    public Mono<HomeConfigurationResponse> getHomeConfiguration() {
        log.info("Requesting of home configuration");

        return Mono.justOrEmpty(
            HomeConfigurationResponse.builder()
                .roomNumber(home.getRooms().size())
                .rooms(home.getRooms().stream()
                    .map(RoomMapper::map)
                    .toList())
                .build()
        );
    }
}
