package cloud.cholewa.heating.pump.service;

import cloud.cholewa.heating.model.HotWater;
import cloud.cholewa.heating.model.Pump;
import cloud.cholewa.heating.shelly.actor.BoilerPro4Client;
import cloud.cholewa.shelly.model.ShellyPro4StatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static cloud.cholewa.heating.model.HeatingTemperatures.HOT_WATER_HIGH_TEMPERATURE;
import static cloud.cholewa.heating.model.HeatingTemperatures.HOT_WATER_LOW_TEMPERATURE;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotWaterPumpService {

    private final HotWater hotWater;
    private final Pump hotWaterPump;
    private final Pump heatingPump;
    private final BoilerPro4Client boilerPro4Client;
    private final HeatingPumpService heatingPumpService;

    @EventListener(ApplicationReadyEvent.class)
    void pumpOff() {
        queryHotWaterPumpStatus().flatMap(response -> turnOffHotWaterPump()).subscribe();
    }

    public Mono<Void> handleHotWaterPump() {
        return queryHotWaterPumpStatus().flatMap(response -> controlHotWaterPump());
    }

    public Mono<ShellyPro4StatusResponse> queryHotWaterPumpStatus() {
        return boilerPro4Client.getHotWaterPumpStatus()
            .doOnNext(response -> {
                hotWaterPump.setRunning(Boolean.TRUE.equals(response.getOutput()));
                log.info("Pump status [HOT_WATER] isWorking: [{}]", hotWaterPump.isRunning());
            });
    }

    private Mono<Void> controlHotWaterPump() {
        if (hotWater.temperature().getValue() < HOT_WATER_LOW_TEMPERATURE) {
            return optionallyTurnOffHeatingPump().then(turnOnHotWaterPump());
        } else if (hotWater.temperature().getValue() >= HOT_WATER_HIGH_TEMPERATURE) {
            return turnOffHotWaterPump();
        }
        return Mono.empty();
    }

    private Mono<Boolean> optionallyTurnOffHeatingPump() {
        if (heatingPump.isRunning()) {
            return heatingPumpService.turnOffHeatingPump("hot water pump is working")
                .then(Mono.defer(heatingPumpService::queryHeatingPumpStatus))
                .doOnNext(response -> heatingPump.setRunning(Boolean.TRUE.equals(response.getOutput())))
                .then(Mono.just(heatingPump.isRunning()));
        }
        return Mono.just(false);
    }

    private Mono<Void> turnOnHotWaterPump() {
        if (!hotWaterPump.isRunning() && hotWater.temperature().getUpdatedAt() != null) {
            return boilerPro4Client.controlHotWaterPump(true)
                .doOnError(throwable -> log.error("Error while turning on hot water pump", throwable))
                .doOnNext(response -> {
                    log.info(
                        "Turned on: [{}], temperature: [{}]",
                        hotWaterPump.getName(),
                        hotWater.temperature().getValue()
                    );
                    hotWaterPump.setRunning(Boolean.TRUE.equals(response.getIson()));
                    hotWaterPump.setStartedAt(LocalDateTime.now());
                })
                .then();
        }
        return Mono.empty();
    }

    private Mono<Void> turnOffHotWaterPump() {
        if (hotWaterPump.isRunning()) {
            return boilerPro4Client.controlHotWaterPump(false)
                .doOnError(throwable -> log.error("Error while turning off hot water pump", throwable))
                .doOnNext(response -> {
                    log.info(
                        "Turned off: [{}], temperature: [{}]",
                        hotWaterPump.getName(),
                        hotWater.temperature().getValue()
                    );
                    hotWaterPump.setRunning(Boolean.TRUE.equals(response.getIson()));
                    hotWaterPump.setStoppedAt(LocalDateTime.now());
                })
                .then();
        }
        return Mono.empty();
    }
}
