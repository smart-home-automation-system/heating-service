package cloud.cholewa.heating.api;

import cloud.cholewa.heating.model.HeatingStatusReply;
import cloud.cholewa.heating.service.HeatingStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HeatingStatusController {

    private final HeatingStatusService service;

    @GetMapping
    Mono<ResponseEntity<HeatingStatusReply>> queryHeatingStatusEnabled() {
        return service.getHeatingStatusEnabled()
            .doOnSubscribe(subscription -> log.info("Incoming request to query heating status"))
            .map(ResponseEntity::ok);
    }

    @PostMapping
    Mono<ResponseEntity<HeatingStatusReply>> updateHeatingStatusEnabled(@RequestParam final String turn) {
        return service.updateHeatingStatusEnabled(turn)
            .doOnSubscribe(subscription -> log.info("Incoming request to update heating status"))
            .map(ResponseEntity::ok);
    }
}
