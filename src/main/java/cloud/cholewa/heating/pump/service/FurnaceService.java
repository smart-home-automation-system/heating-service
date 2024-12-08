package cloud.cholewa.heating.pump.service;

import cloud.cholewa.heating.model.Furnace;
import cloud.cholewa.heating.model.Pump;
import cloud.cholewa.heating.shelly.actor.BoilerPro4Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FurnaceService {

    private final Pump hotWaterPump;
    private final Pump heatingPump;
    private final Furnace furnace;

    private final BoilerPro4Client boilerPro4Client;

    public Mono<Void> handleFurnace() {
        if (heatingPump.isRunning() || hotWaterPump.isRunning()) {
            turnOnFurnace();
        } else {
            turnOffFurnace();
        }

        return Mono.empty();
    }

    private void turnOnFurnace() {
        if (!furnace.isRunning()) {
            boilerPro4Client.controlFurnace(true)
                .doOnError(throwable -> log.error("Error while turning on furnace", throwable))
                .doOnNext(response -> {
                    log.info(
                        "Furnace turned on due to heating: [{}] or hot water: [{}]",
                        heatingPump.isRunning(), hotWaterPump.isRunning()
                    );
                    furnace.setStartedAt(LocalDateTime.now());
                })
                .subscribe();
        }
    }

    private void turnOffFurnace() {
        if (furnace.isRunning()) {
            boilerPro4Client.controlFurnace(false)
                .doOnError(throwable -> log.error("Error while turning off furnace", throwable))
                .doOnNext(response -> {
                    log.info("Furnace turned off");
                    furnace.setStoppedAt(LocalDateTime.now());
                })
                .subscribe();
        }
    }
}
