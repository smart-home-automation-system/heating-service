package cloud.cholewa.heating;

import cloud.cholewa.home.model.SystemActiveReply;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
public class HeatingController {

    @GetMapping("/status/active")
    Mono<ResponseEntity<SystemActiveReply>> querySystemActive() {
        return Mono.just(SystemActiveReply.builder().active(false).build())
            .doOnSubscribe(subscription -> log.info("Querying system active status"))
            .map(ResponseEntity::ok);
    }
}
