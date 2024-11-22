//package cloud.cholewa.heating.home.api;
//
//import cloud.cholewa.heating.home.model.HomeConfigurationResponse;
//import cloud.cholewa.heating.home.model.RoomConfigurationResponse;
//import cloud.cholewa.heating.home.service.HomeService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//import reactor.core.publisher.Mono;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("heating")
//@Slf4j
//public class HomeController {
//
//    private final HomeService homeService;
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
//}
