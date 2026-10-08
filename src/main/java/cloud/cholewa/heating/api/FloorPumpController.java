package cloud.cholewa.heating.api;

import cloud.cholewa.heating.model.FloorPumpReply;
import cloud.cholewa.heating.service.FloorPumpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FloorPumpController {

    private final FloorPumpService floorPumpService;

    @GetMapping("/floor-pump")
    Mono<ResponseEntity<FloorPumpReply>> queryFloorPump() {
        return floorPumpService.queryFloorPump()
            .doOnSubscribe(subscription -> log.info("Querying the state of the floor pump"))
            .map(ResponseEntity::ok);
    }
}
