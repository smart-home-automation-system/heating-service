package cloud.cholewa.heating.home.service;

import cloud.cholewa.heating.home.api.mapper.HomeMapper;
import cloud.cholewa.heating.home.api.model.HomeShortResponse;
import cloud.cholewa.heating.model.Home;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomeService {

    private final Home home;

    public Mono<Home> getHomeStats() {
        return Mono.just(home);
    }

    public Mono<HomeShortResponse> getHomeShortStats() {
        return Mono.justOrEmpty(HomeMapper.toHomeShortResponse(home));
    }

//    public Mono<RoomConfigurationResponse> getRoomConfiguration(final String roomName) {
//        log.info("Requesting of room configuration for room {}", roomName);
//
//        return Mono.just(
//                home.getRooms().stream()
//                    .filter(room -> room.getName().name().equalsIgnoreCase(roomName))
//                    .findAny()
//            )
//            .flatMap(room -> room.map(Mono::just)
//                .orElseThrow(() -> new RoomNotFoundException(roomName)))
//            .map(RoomMapper::map);
//    }
//
//    public Mono<HomeConfigurationResponse> getHomeConfiguration() {
//        log.info("Requesting of home configuration");
//
//        return Mono.justOrEmpty(
//            HomeConfigurationResponse.builder()
//                .isHeatingAllowed(home.isHeatingAllowed())
//                .boilerRoom(home.getBoiler())
//                .roomNumber(home.getRooms().size())
//                .rooms(home.getRooms().stream()
//                    .map(RoomMapper::map)
//                    .toList())
//                .build()
//        );
//    }
//
//    public Mono<HomeConfigurationResponse> changeHomeHeatingState(final String mode) {
//        switch (mode) {
//            case "on" -> home.setHeatingAllowed(true);
//            case "off" -> home.setHeatingAllowed(false);
//        }
//
//        return Mono.justOrEmpty(
//            HomeConfigurationResponse.builder()
//                .isHeatingAllowed(home.isHeatingAllowed())
//                .boilerRoom(home.getBoiler())
//                .roomNumber(home.getRooms().size())
//                .rooms(home.getRooms().stream()
//                    .map(RoomMapper::map)
//                    .toList())
//                .build()
//        );
//    }
}
