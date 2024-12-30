package cloud.cholewa.heating.pump.service;

import cloud.cholewa.heating.model.BoilerRoom;
import cloud.cholewa.heating.model.Fireplace;
import cloud.cholewa.heating.model.HeatingTemperatures;
import cloud.cholewa.heating.model.Pump;
import cloud.cholewa.heating.model.Room;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeatingPumpService {

    private final BoilerRoom boilerRoom;
    private final Fireplace fireplace;
    private final Pump heatingPump;
    private final Pump hotWaterPump;
    private final List<Room> rooms;

    private final BoilerPro4Client boilerPro4Client;

    @EventListener(ApplicationReadyEvent.class)
    void pumpOff() {
        queryHeatingPumpStatus()
            .delayElement(Duration.ofSeconds(1))
            .flatMap(response ->  turnOffHeatingPump(response, "system startup"))
            .subscribe();
    }

    public Mono<ShellyPro4StatusResponse> handleHeatingPump() {
        return queryHeatingPumpStatus()
            .delayElement(Duration.ofSeconds(1))
            .flatMap(this::controlHeatingPump);
    }

    public Mono<ShellyPro4StatusResponse> queryHeatingPumpStatus() {
        return boilerPro4Client.getHeatingPumpStatus()
            .doOnNext(response -> {
                heatingPump.setRunning(Boolean.TRUE.equals(response.getOutput()));
                log.info("Pump status [HEATING PUMP] isWorking: [{}]", response.getOutput());
            });
    }

    private Mono<ShellyPro4StatusResponse> controlHeatingPump(final ShellyPro4StatusResponse response) {
        if (boilerRoom.isHeatingEnabled() && isNotFireplaceActive() && isAnyRoomHeatingActive() && !hotWaterPump.isRunning()) {
            return turnOnHeatingPump(response);
        } else {
            return turnOffHeatingPump(response, "no room to heat");
        }
    }

    private boolean isAnyRoomHeatingActive() {
        return rooms.stream().anyMatch(Room::isHeatingActive);
    }

    private boolean isNotFireplaceActive() {
        return fireplace.temperature().getValue() < HeatingTemperatures.FIREPLACE_TEMPERATURE_VALID_TO_ENABLE_FURNACE;
    }

    private Mono<ShellyPro4StatusResponse> turnOnHeatingPump(final ShellyPro4StatusResponse shellyPro4StatusResponse) {
        if (!heatingPump.isRunning()) {
            return boilerPro4Client.controlHeatingPump(true)
                .doOnError(throwable -> log.error("Error while turning on heating pump", throwable))
                .doOnNext(response -> {
                    log.info("Heating pump turned on - some rooms are ready to heat");
                    heatingPump.setStartedAt(LocalDateTime.now());
                })
                .delayElement(Duration.ofSeconds(1))
                .then(queryHeatingPumpStatus());
        }
        return Mono.just(shellyPro4StatusResponse);
    }

    public Mono<ShellyPro4StatusResponse> turnOffHeatingPump(final ShellyPro4StatusResponse shellyPro4StatusResponse, final String messageReason) {
        if (heatingPump.isRunning()) {
            return boilerPro4Client.controlHeatingPump(false)
                .doOnError(throwable -> log.error("Error while turning off heating pump", throwable))
                .doOnNext(response -> {
                    log.info("Heating pump turned off - {}", messageReason);
                    heatingPump.setStoppedAt(LocalDateTime.now());
                })
                .delayElement(Duration.ofSeconds(1))
                .then(queryHeatingPumpStatus());
        }
        return Mono.just(shellyPro4StatusResponse);
    }
}
