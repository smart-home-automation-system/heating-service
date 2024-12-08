package cloud.cholewa.heating.pump.cron;

import cloud.cholewa.heating.pump.service.FireplacePumpService;
import cloud.cholewa.heating.pump.service.FloorPumpService;
import cloud.cholewa.heating.pump.service.FurnaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class PumpScheduler {

    private final FireplacePumpService fireplacePumpService;
    private final FurnaceService furnaceService;
    private final FloorPumpService floorPumpService;

    @Scheduled(fixedRateString = "${jobs.pumps.poolingInterval}", initialDelayString = "PT15s")
    void handleBoiler() {
        log.info("Updating boiler devices status ...");
        Flux.interval(Duration.ofSeconds(3))
            .take(3)
            .flatMap(i ->
                switch (i.intValue() + 1) {
                    case 1 -> fireplacePumpService.handleFireplacePump();
                    case 2 -> floorPumpService.handleFloorPump();
                    case 3 -> furnaceService.handleFurnace();
                    default -> Mono.empty();
                })
            .subscribe();
    }
}
