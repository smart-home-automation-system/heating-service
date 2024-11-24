package cloud.cholewa.heating.pump.cron;

import cloud.cholewa.heating.model.Furnace;
import cloud.cholewa.heating.model.Pump;
import cloud.cholewa.heating.pump.service.FireplacePumpService;
import cloud.cholewa.heating.pump.service.FurnaceService;
import cloud.cholewa.heating.pump.service.HeatingPumpService;
import cloud.cholewa.heating.pump.service.HotWaterPumpService;
import cloud.cholewa.heating.shelly.actor.BoilerPro4Client;
import cloud.cholewa.heating.shelly.actor.HeaterPro4Client;
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

    private final Furnace furnace;
    private final Pump fireplacePump;
    private final Pump floorPump;
    private final Pump heatingPump;
    private final Pump hotWaterPump;

    private final BoilerPro4Client boilerPro4Client;
    private final HeaterPro4Client heaterPro4Client;

    private final HotWaterPumpService hotWaterPumpService;
    private final FireplacePumpService fireplacePumpService;
    private final HeatingPumpService heatingPumpService;
    private final FurnaceService furnaceService;

    @Scheduled(fixedRateString = "${jobs.pumps.poolingInterval}", initialDelayString = "PT5s")
    void handleBoiler() {
        queryPumpStatus();
        hotWaterPumpService.handleHotWaterPump();
        fireplacePumpService.handleFireplacePump();
        heatingPumpService.handleHeatingPump();
        furnaceService.handleFurnace();
    }

    private void queryPumpStatus() {
        Flux.interval(Duration.ofSeconds(3))
            .take(5)
            .flatMap(i ->
                switch (i.intValue() + 1) {
                    case 1 -> queryHotWaterPumpStatus();
                    case 2 -> queryHeatingPumpStatus();
                    case 3 -> queryFireplacePumpStatus();
                    case 4 -> queryFloorPumpStatus();
                    case 5 -> queryFurnaceStatus();
                    default -> Mono.empty();
                })
            .subscribe();
    }

    private Mono<Void> queryHotWaterPumpStatus() {
        return boilerPro4Client.getHotWaterPumpStatus()
            .doOnError(throwable -> log.error("Error while querying hot water pump status", throwable))
            .flatMap(response -> {
                    hotWaterPump.setRunning(Boolean.TRUE.equals(response.getOutput()));
                    return Mono.empty();
                }
            );
    }

    private Mono<Void> queryHeatingPumpStatus() {
        return boilerPro4Client.getHeatingPumpStatus()
            .doOnError(throwable -> log.error("Error while querying hot water pump status", throwable))
            .flatMap(response -> {
                    heatingPump.setRunning(Boolean.TRUE.equals(response.getOutput()));
                    return Mono.empty();
                }
            );
    }

    private Mono<Void> queryFireplacePumpStatus() {
        return boilerPro4Client.getFireplacePumpStatus()
            .doOnError(throwable -> log.error("Error while querying fireplace pump status", throwable))
            .flatMap(response -> {
                    fireplacePump.setRunning(Boolean.TRUE.equals(response.getOutput()));
                    return Mono.empty();
                }
            );
    }

    private Mono<Void> queryFloorPumpStatus() {
        return heaterPro4Client.getFloorPumpStatus()
            .doOnError(throwable -> log.error("Error while querying floor pump status", throwable))
            .flatMap(response -> {
                    floorPump.setRunning(Boolean.TRUE.equals(response.getOutput()));
                    return Mono.empty();
                }
            );
    }

    private Mono<Void> queryFurnaceStatus() {
        return boilerPro4Client.getFurnaceStatus()
            .doOnError(throwable -> log.error("Error while querying furnace status", throwable))
            .flatMap(response -> {
                    furnace.setRunning(Boolean.TRUE.equals(response.getOutput()));
                    return Mono.empty();
                }
            );
    }
}
