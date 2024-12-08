package cloud.cholewa.heating.pump.service;

import cloud.cholewa.heating.model.Fireplace;
import cloud.cholewa.heating.model.Pump;
import cloud.cholewa.heating.shelly.actor.BoilerPro4Client;
import cloud.cholewa.shelly.model.ShellyPro4StatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static cloud.cholewa.heating.model.HeatingTemperatures.FIREPLACE_ALERT_TEMPERATURE;
import static cloud.cholewa.heating.model.HeatingTemperatures.FIREPLACE_START_TEMPERATURE;

@Slf4j
@Service
@RequiredArgsConstructor
public class FireplacePumpService {

    private final Fireplace fireplace;
    private final Pump fireplacePump;

    private final BoilerPro4Client boilerPro4Client;

    public Mono<Void> handleFireplacePump() {
        return queryFireplacePumpStatus()
            .flatMap(response -> controlFireplacePump());
    }

    private Mono<ShellyPro4StatusResponse> queryFireplacePumpStatus() {
        return boilerPro4Client.getFireplacePumpStatus()
            .doOnNext(response -> {
                fireplacePump.setRunning(Boolean.TRUE.equals(response.getOutput()));
                log.info("Pump status [FIREPLACE] isWorking: [{}]", response.getOutput());
            });
    }

    private Mono<Void> controlFireplacePump() {
        if (fireplace.temperature().getValue() > FIREPLACE_ALERT_TEMPERATURE) {
            log.error("ALERT!!! --- Boiler has exceeded alert temperature: [{} C]", fireplace.temperature().getValue());
            //TODO send notify and open heater valves???
            //TODO open heaters in bathrooms
        } else if (fireplace.temperature().getValue() >= FIREPLACE_START_TEMPERATURE) {
            return turnOnFireplacePump();
        } else if (fireplace.temperature().getValue() < FIREPLACE_START_TEMPERATURE) {
            return turnOffFireplacePump();
        }

        return Mono.empty();
    }

    private Mono<Void> turnOnFireplacePump() {
        if (!fireplacePump.isRunning()) {
            return boilerPro4Client.controlFireplacePump(true)
                .doOnError(throwable -> log.error("Error while turning on fireplace pump", throwable))
                .doOnNext(response -> {
                    log.info(
                        "Fireplace pump turned on, temperature is: [{} C]",
                        fireplace.temperature().getValue()
                    );
                    fireplacePump.setStartedAt(LocalDateTime.now());
                })
                .then();
        }
        return Mono.empty();
    }

    private Mono<Void> turnOffFireplacePump() {
        if (fireplacePump.isRunning() && fireplace.temperature().getUpdatedAt() != null) {
            return boilerPro4Client.controlFireplacePump(false)
                .doOnError(throwable -> log.error("Error while turning off fireplace pump", throwable))
                .doOnNext(response -> {
                    log.info(
                        "Fireplace pump turned off, temperature is: [{} C]",
                        fireplace.temperature().getValue()
                    );
                    fireplacePump.setStoppedAt(LocalDateTime.now());
                })
                .then();
        }
        return Mono.empty();
    }
}
