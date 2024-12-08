package cloud.cholewa.heating.pump.service;

import cloud.cholewa.heating.model.HotWater;
import cloud.cholewa.heating.model.Pump;
import cloud.cholewa.heating.shelly.actor.BoilerPro4Client;
import cloud.cholewa.shelly.model.ShellyPro4StatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final BoilerPro4Client boilerPro4Client;
    private final HeatingPumpService heatingPumpService;

    public Mono<Void> handleHotWaterPump() {
        return queryHotWaterPumpStatus()
            .flatMap(response -> controlHowWaterPump());
    }

    public Mono<ShellyPro4StatusResponse> queryHotWaterPumpStatus() {
        return boilerPro4Client.getHotWaterPumpStatus()
            .doOnNext(response -> {
                hotWaterPump.setRunning(Boolean.TRUE.equals(response.getOutput()));
                log.info("Pump status [HOT_WATER] isWorking: [{}]", response.getOutput());
            });
    }

    private Mono<Void> controlHowWaterPump() {
        if (hotWater.temperature().getValue() < HOT_WATER_LOW_TEMPERATURE) {
            return heatingPumpService.queryHeatingPumpStatus()
                .flatMap(response -> {
                    if (Boolean.TRUE.equals(response.getOutput())) {
                        return heatingPumpService.turnOffHeatingPump("hot water pump is working");
                    } else {
                        return turnOnHotWaterPump();
                    }
                });
        } else if (hotWater.temperature().getValue() > HOT_WATER_HIGH_TEMPERATURE) {
            return turnOffHotWaterPump();
        }
        return Mono.empty();
    }

    private Mono<Void> turnOnHotWaterPump() {
        if (!hotWaterPump.isRunning() && hotWater.temperature().getUpdatedAt() != null) {
            return boilerPro4Client.controlHotWaterPump(true)
                .doOnError(throwable -> log.error("Error while turning on hot water pump", throwable))
                .doOnNext(response -> {
                    log.info("Turned on: [{}], temperature: [{}]", hotWaterPump.getName(), hotWater.temperature().getValue());
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
                    log.info("Turned off: [{}], temperature: [{}]", hotWaterPump.getName(), hotWater.temperature().getValue());
                    hotWaterPump.setStoppedAt(LocalDateTime.now());
                })
                .then();
        }
        return Mono.empty();
    }
}
