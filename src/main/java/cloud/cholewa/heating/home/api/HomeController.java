package cloud.cholewa.heating.home.api;

import cloud.cholewa.heating.home.service.HomeService;
import cloud.cholewa.heating.model.BoilerRoom;
import cloud.cholewa.heating.model.Home;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
        return homeService.getHomeStats().map(ResponseEntity::ok);
    }

    @GetMapping("/status:short")
    Mono<ResponseEntity<Void>> getHomeShortStatus(@RequestBody(required = false) final String fakeRequestBody) {
        log.info("Requesting short home status");
        return Mono.just(new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED));
    }

    @GetMapping("/status/boiler")
    Mono<ResponseEntity<BoilerRoom>> getBoilerRoomStatus(@RequestBody(required = false) final String fakeRequestBody) {
        log.info("Requesting boiler room status");
        return Mono.just(new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED));
    }

//
//    @GetMapping("configuration/room")
//    Mono<ResponseEntity<RoomConfigurationResponse>> getRoomConfiguration(
//        @RequestBody(required = false) final String fakeRequestDueLoggingIssue,
//        @RequestParam final String roomName
//    ) {
//        log.info("Getting room configuration for room {}", roomName);
//        return homeService.getRoomConfiguration(roomName)
//            .map(ResponseEntity::ok)
//            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
//    }
//
//    @GetMapping("configuration/home")
//    Mono<ResponseEntity<HomeConfigurationResponse>> getHomeConfiguration(
//        @RequestBody(required = false) final String fakeRequestDueLoggingIssue
//    ) {
//        log.info("Getting home configuration");
//        return homeService.getHomeConfiguration().map(ResponseEntity::ok);
//    }
//
//    @GetMapping
//    Mono<ResponseEntity<HomeConfigurationResponse>> changeHeatingAllowed(@RequestParam final String heating) {
//        log.info("Changing heating allowed to: {}", heating);
//
//        return heating.equals("on") || heating.equals("off")
//            ? homeService.changeHomeHeatingState(heating).map(ResponseEntity::ok)
//            : Mono.error(new RuntimeException("Invalid heating value: " + heating));
//    }
}
