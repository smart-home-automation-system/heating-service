package cloud.cholewa.heating.hot.water.api;

import cloud.cholewa.heating.hot.water.service.HotWaterService;
import cloud.cholewa.heating.model.HotWater;
import cloud.cholewa.heating.model.WaterCirculation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("water")
@RequiredArgsConstructor
public class HowWaterController {

    private final HotWaterService hotWaterService;

    @GetMapping("status")
    Mono<ResponseEntity<HotWater>> getStatus() {
        log.info("Requesting hot water status");
        return hotWaterService.getHotWaterStatus().map(ResponseEntity::ok);
    }

    @GetMapping("/circulation/status")
    Mono<ResponseEntity<WaterCirculation>> getCirculationStatus() {
        log.info("Requesting hot water circulation status");
        return hotWaterService.getWaterCirculationStatus().map(ResponseEntity::ok);
    }

    @PostMapping("update")
    Mono<ResponseEntity<String>> executeHotWaterUpdate() {
        log.info("Executing manual hot water update");
        return hotWaterService.executeHotWaterUpdate()
            .flatMap(response ->  Mono.just(ResponseEntity.ok().body("Updated")));
    }
}

