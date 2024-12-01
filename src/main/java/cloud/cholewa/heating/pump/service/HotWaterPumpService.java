package cloud.cholewa.heating.pump.service;

import cloud.cholewa.heating.model.HotWater;
import cloud.cholewa.heating.model.Pump;
import cloud.cholewa.heating.shelly.actor.BoilerPro4Client;
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

    public Mono<Void> handleHotWaterPump() {
        if (hotWater.temperature().getValue() < HOT_WATER_LOW_TEMPERATURE) {
            turnOnHotWaterPump();
        } else if (hotWater.temperature().getValue() > HOT_WATER_HIGH_TEMPERATURE) {
            turnOffHotWaterPump();
        }
        return Mono.empty();
    }

    private void turnOnHotWaterPump() {
        if (!hotWaterPump.isRunning() && hotWater.temperature().getUpdatedAt() != null) {
            boilerPro4Client.controlHotWaterPump(true)
                .doOnError(throwable -> log.error("Error while turning on hot water pump", throwable))
                .doOnNext(response -> {
                    log.info("[{}] pump turned on", hotWaterPump.getName());
                    hotWaterPump.setStartedAt(LocalDateTime.now());
                })
                .subscribe();
        }
    }

    private void turnOffHotWaterPump() {
        if (hotWaterPump.isRunning()) {
            boilerPro4Client.controlHotWaterPump(false)
                .doOnError(throwable -> log.error("Error while turning off hot water pump", throwable))
                .doOnNext(response -> {
                    log.info("[{}] pump turned off", hotWaterPump.getName());
                    hotWaterPump.setStoppedAt(LocalDateTime.now());
                })
                .subscribe();
        }
    }
}
