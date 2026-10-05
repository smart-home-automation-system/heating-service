package cloud.cholewa.heating.api;

import cloud.cholewa.heating.model.TemperatureSensorReply;
import cloud.cholewa.heating.service.TemperatureSensorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TemperatureSensorController {

    private final TemperatureSensorService temperatureSensorService;

    @GetMapping("/temperature/sensors")
    Mono<ResponseEntity<List<TemperatureSensorReply>>> queryTemperatureSensors() {
        return temperatureSensorService.querySensors()
            .collectList()
            .doOnSubscribe(subscription -> log.info("Querying the last readings of the temperature sensors"))
            .map(ResponseEntity::ok);
    }
}
