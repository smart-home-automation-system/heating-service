package cloud.cholewa.heating.pump.service;

import cloud.cholewa.heating.model.Fireplace;
import cloud.cholewa.heating.model.Pump;
import cloud.cholewa.heating.shelly.actor.BoilerPro4Client;
import cloud.cholewa.shelly.model.ShellyPro4StatusResponse;
import cloud.cholewa.shelly.model.ShellyProRelayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
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
            .delayElement(Duration.ofSeconds(1))
            .then(controlFireplacePump());
    }

    private Mono<ShellyPro4StatusResponse> queryFireplacePumpStatus() {
        return boilerPro4Client.getFireplacePumpStatus()
            .doOnNext(response -> {
                fireplacePump.setRunning(Boolean.TRUE.equals(response.getOutput()));
                log.info("Pump status [FIREPLACE] isWorking: [{}]", fireplacePump.isRunning());
            });
    }

    private Mono<Void> controlFireplacePump() {
        if (fireplace.temperature().getValue() > FIREPLACE_ALERT_TEMPERATURE) {
            return processEmergencySituation();
        } else if (fireplace.temperature().getValue() >= FIREPLACE_START_TEMPERATURE) {
            return turnOnFireplacePump();
        } else if (fireplace.temperature().getValue() < FIREPLACE_START_TEMPERATURE) {
            return turnOffFireplacePump();
        }
        return Mono.empty();
    }

    private Mono<Void> processEmergencySituation() {
        return turnOnFireplacePump()
            .then(Mono.fromRunnable(() -> log.error(
                "ALERT!!! --- Boiler has exceeded alert temperature: [{} C]",
                fireplace.temperature().getValue()
            )));
        //TODO send notify and open heater valves???
        //open heaters in bathrooms
        //any CustomEvent??
    }

    private Mono<Void> turnOnFireplacePump() {
        if (!fireplacePump.isRunning()) {
            return boilerPro4Client.controlFireplacePump(true)
                .doOnError(this::logErrorStatus)
                .doOnNext(response -> {
                    logFireplaceStatus(response);
                    fireplacePump.setStartedAt(LocalDateTime.now());
                })
                .then();
        }
        return Mono.empty();
    }

    private Mono<Void> turnOffFireplacePump() {
        if (fireplacePump.isRunning() && fireplace.temperature().getUpdatedAt() != null) {
            return boilerPro4Client.controlFireplacePump(false)
                .doOnError(this::logErrorStatus)
                .doOnNext(response -> {
                    logFireplaceStatus(response);
                    fireplacePump.setStoppedAt(LocalDateTime.now());
                })
                .then();
        }
        return Mono.empty();
    }

    private void logFireplaceStatus(final ShellyProRelayResponse response) {
        log.info(
            "[FIREPLACE] pump turned: [{}], temperature: [{} C]",
            Boolean.TRUE.equals(response.getIson()) ? "ON" : "OFF",
            fireplace.temperature().getValue()
        );
    }

    private void logErrorStatus(final Throwable throwable) {
        log.error("Error while turning controlling fireplace pump", throwable);
    }
}
