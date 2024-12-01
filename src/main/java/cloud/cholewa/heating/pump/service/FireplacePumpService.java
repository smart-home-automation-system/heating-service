package cloud.cholewa.heating.pump.service;

import cloud.cholewa.heating.model.Fireplace;
import cloud.cholewa.heating.model.Pump;
import cloud.cholewa.heating.shelly.actor.BoilerPro4Client;
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
        if (fireplace.temperature().getValue() >= FIREPLACE_START_TEMPERATURE) {
            turnOnFireplacePump();
        } else if (fireplace.temperature().getValue() < FIREPLACE_START_TEMPERATURE) {
            turnOffFireplacePump();
        }

        if (fireplace.temperature().getValue() > FIREPLACE_ALERT_TEMPERATURE) {
            log.error("ALERT!!! --- Boiler has exceeded alert temperature: [{} C]", fireplace.temperature().getValue());
            //TODO send notify and open heater valves???
            //TODO open heaters in bathrooms
        }

        return Mono.empty();
    }

    private void turnOnFireplacePump() {
        if (!fireplacePump.isRunning()) {
            boilerPro4Client.controlFireplacePump(true)
                .doOnError(throwable -> log.error("Error while turning on fireplace pump", throwable))
                .doOnNext(response -> {
                    log.info(
                        "Fireplace pump turned on, temperature is: [{} C]",
                        fireplace.temperature().getValue()
                    );
                    fireplacePump.setStartedAt(LocalDateTime.now());
                })
                .subscribe();
        }
    }

    private void turnOffFireplacePump() {
        if (fireplacePump.isRunning() && fireplace.temperature().getUpdatedAt() != null) {
            boilerPro4Client.controlFireplacePump(false)
                .doOnError(throwable -> log.error("Error while turning off fireplace pump", throwable))
                .doOnNext(response -> {
                    log.info(
                        "Fireplace pump turned off, temperature is: [{} C]",
                        fireplace.temperature().getValue()
                    );
                    fireplacePump.setStoppedAt(LocalDateTime.now());
                })
                .subscribe();
        }
    }
}
