package cloud.cholewa.heating.home.api;

import cloud.cholewa.heating.home.api.model.HomeShortResponse;
import cloud.cholewa.heating.home.service.HomeService;
import cloud.cholewa.heating.model.BoilerRoom;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Room;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/status")
    Mono<ResponseEntity<Home>> getHomeStatus(@RequestBody(required = false) final String fakeRequestBody) {
        log.info("Requesting full home status");
        return homeService.getHomeStatus().map(ResponseEntity::ok);
    }

    @GetMapping("/status:short")
    Mono<ResponseEntity<HomeShortResponse>> getHomeShortStatus(@RequestBody(required = false) final String fakeRequestBody) {
        log.info("Requesting short home status");
        return homeService.getHomeShortStatus().map(ResponseEntity::ok);
    }

    @GetMapping("/status/boiler")
    Mono<ResponseEntity<BoilerRoom>> getBoilerRoomStatus(@RequestBody(required = false) final String fakeRequestBody) {
        log.info("Requesting boiler room status");
        return Mono.just(new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED));
    }

    @GetMapping("/status/room/{name}")
    Mono<ResponseEntity<Room>> getRoomStatus(
        @PathVariable String name,
        @RequestBody(required = false) final String fakeRequestBody
    ) {
        log.info("Requesting status for room: [{}]", name.toUpperCase());
        return homeService.getRoomStatusByName(name).map(ResponseEntity::ok);
    }
}
