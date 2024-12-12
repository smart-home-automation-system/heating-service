package cloud.cholewa.heating.home.service;

import cloud.cholewa.heating.home.api.mapper.HomeMapper;
import cloud.cholewa.heating.home.api.model.HomeShortResponse;
import cloud.cholewa.heating.infrastructure.error.RoomNotFoundException;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Room;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomeService {

    private final Home home;

    public Mono<Home> getHomeStatus() {
        return Mono.just(home);
    }

    public Mono<HomeShortResponse> getHomeShortStatus() {
        return Mono.justOrEmpty(HomeMapper.toHomeShortResponse(home));
    }

    public Mono<Room> getRoomStatusByName(final String name) {
        return Flux.fromIterable(home.rooms())
            .filter(room -> room.getName().name().replace("_", "").equalsIgnoreCase(name))
            .doOnNext(room -> log.info("Founded room with name: [{}]", room.getName()))
            .singleOrEmpty()
            .switchIfEmpty(Mono.error(new RoomNotFoundException("[ " + name.toUpperCase() + " ]" + " not found")));
    }

    public Mono<Home> setHomeHeating(final boolean heatingEnable) {
        home.boilerRoom().setHeatingEnabled(heatingEnable);
        return Mono.just(home);
    }
}
