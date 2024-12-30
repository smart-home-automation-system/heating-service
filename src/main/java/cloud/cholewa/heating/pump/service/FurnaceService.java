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

import java.time.Duration;
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
        queryFurnaceStatus()
            .delayElement(Duration.ofSeconds(1))
            .flatMap(response ->  turnOffFurnace(response, "system startup"))
            .subscribe();
    }

    public Mono<ShellyPro4StatusResponse> handleFurnace() {
        return queryFurnaceStatus()
            .delayElement(Duration.ofSeconds(1))
            .flatMap(response -> {
                if (heatingPump.isRunning() || hotWaterPump.isRunning()) {
                    return turnOnFurnace(response);
                } else {
                    return turnOffFurnace(response, "heating not required");
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

    private Mono<ShellyPro4StatusResponse> turnOnFurnace(final ShellyPro4StatusResponse shellyPro4StatusResponse) {
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
                .delayElement(Duration.ofSeconds(1))
                .then(queryFurnaceStatus());
        }
        return Mono.just(shellyPro4StatusResponse);
    }

    private Mono<ShellyPro4StatusResponse> turnOffFurnace(final ShellyPro4StatusResponse shellyPro4StatusResponse, final String messageReason) {
        if (furnace.isRunning()) {
            return boilerPro4Client.controlFurnace(false)
                .doOnError(throwable -> log.error("Error while turning off furnace", throwable))
                .doOnNext(response -> {
                    log.info("Furnace turned off, due to: [{}]", messageReason);
                    furnace.setStoppedAt(LocalDateTime.now());
                })
                .delayElement(Duration.ofSeconds(1))
                .then(queryFurnaceStatus());
        }
        return Mono.just(shellyPro4StatusResponse);
    }
}
