package cloud.cholewa.heating.pump.service;

import cloud.cholewa.heating.model.Furnace;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class FurnaceService {

    private final Pump hotWaterPump;
    private final Pump heatingPump;
    private final Furnace furnace;

    private final BoilerPro4Client boilerPro4Client;

    @EventListener(ApplicationReadyEvent.class)
    void furnaceOff() {
        queryFurnaceStatus().then(turnOffFurnace("system startup")).subscribe();
    }

    public Mono<Void> handleFurnace() {
        return queryFurnaceStatus()
            .flatMap(response -> {
                if (heatingPump.isRunning() || hotWaterPump.isRunning()) {
                    return turnOnFurnace();
                } else {
                    return turnOffFurnace("heating not required");
                }
            });
    }

    private Mono<ShellyPro4StatusResponse> queryFurnaceStatus() {
        return boilerPro4Client.getFurnaceStatus()
            .doOnNext(response -> {
                furnace.setRunning(Boolean.TRUE.equals(response.getOutput()));
                log.info("Status update [FURNACE] isWorking: [{}]", response.getOutput());
            });
    }

    private Mono<Void> turnOnFurnace() {
        if (!furnace.isRunning()) {
            return boilerPro4Client.controlFurnace(true)
                .doOnError(throwable -> log.error("Error while turning on furnace", throwable))
                .doOnNext(response -> {
                    log.info(
                        "Furnace turned on due to heating: [{}] or hot water: [{}]",
                        heatingPump.isRunning(),
                        hotWaterPump.isRunning()
                    );
                    furnace.setStartedAt(LocalDateTime.now());
                })
                .then();
        }
        return Mono.empty();
    }

    private Mono<Void> turnOffFurnace(final String messageReason) {
        if (furnace.isRunning()) {
            return boilerPro4Client.controlFurnace(false)
                .doOnError(throwable -> log.error("Error while turning off furnace", throwable))
                .doOnNext(response -> {
                    log.info("Furnace turned off, due to: [{}]", messageReason);
                    furnace.setStoppedAt(LocalDateTime.now());
                })
                .then();
        }
        return Mono.empty();
    }
}
