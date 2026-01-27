package cloud.cholewa.heating.api;

import cloud.cholewa.heating.service.HeatingService;
import cloud.cholewa.home.model.SystemActiveReply;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HeatingController {

    private final HeatingService heatingService;

    @GetMapping("/status/active")
    Mono<ResponseEntity<SystemActiveReply>> querySystemActive() {
        return heatingService.queryHeatingSystemActive()
            .doOnSubscribe(subscription -> log.info("Querying the heating system for the current system status"))
            .map(ResponseEntity::ok);
    }
}
