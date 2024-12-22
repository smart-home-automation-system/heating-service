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

import java.time.Duration;
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
        queryHotWaterPumpStatus().map(this::turnOffHotWaterPump).subscribe();
    }

    public Mono<ShellyPro4StatusResponse> handleHotWaterPump() {
        return queryHotWaterPumpStatus()
            .delayElement(Duration.ofSeconds(1))
            .flatMap(this::controlHotWaterPump);
    }

    public Mono<ShellyPro4StatusResponse> queryHotWaterPumpStatus() {
        return boilerPro4Client.getHotWaterPumpStatus()
            .doOnNext(response -> {
                hotWaterPump.setRunning(Boolean.TRUE.equals(response.getOutput()));
                log.info("Pump status [HOT_WATER] isWorking: [{}]", hotWaterPump.isRunning());
            });
    }

    private Mono<ShellyPro4StatusResponse> controlHotWaterPump(final ShellyPro4StatusResponse response) {
        if (hotWater.temperature().getValue() < HOT_WATER_LOW_TEMPERATURE) {
            return optionallyTurnOffHeatingPump()
                .then(turnOnHotWaterPump(response));
        } else if (hotWater.temperature().getValue() >= HOT_WATER_HIGH_TEMPERATURE) {
            return optionallyTurnOnHeatingPump()
                .then(turnOffHotWaterPump(response));
        }
        return Mono.just(response);
    }

    private Mono<Void> optionallyTurnOffHeatingPump() {
        return heatingPumpService.turnOffHeatingPump("hot water pump is working")
            .then(Mono.defer(heatingPumpService::queryHeatingPumpStatus))
            .doOnNext(response -> heatingPump.setRunning(Boolean.TRUE.equals(response.getOutput())))
            .then();
    }

    private Mono<ShellyPro4StatusResponse> turnOnHotWaterPump(final ShellyPro4StatusResponse shellyPro4StatusResponse) {
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
                .delayElement(Duration.ofSeconds(1))
                .then(queryHotWaterPumpStatus());
        }
        return Mono.just(shellyPro4StatusResponse);
    }

    private Mono<ShellyPro4StatusResponse> turnOffHotWaterPump(final ShellyPro4StatusResponse shellyPro4StatusResponse) {
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
                .delayElement(Duration.ofSeconds(1))
                .then(queryHotWaterPumpStatus());
        }
        return Mono.just(shellyPro4StatusResponse);
    }

    private Mono<Void> optionallyTurnOnHeatingPump() {
        return heatingPumpService.handleHeatingPump();
    }
}
