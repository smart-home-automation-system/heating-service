package cloud.cholewa.heating.pump.service;

import cloud.cholewa.heating.model.Pump;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.shelly.actor.HeaterPro4Client;
import cloud.cholewa.shelly.model.ShellyPro4StatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FloorPumpService {

    private final Pump floorPump;

    private final Room wardrobe;
    private final Room bathroomUp;

    private final HeaterPro4Client heaterPro4Client;

    public Mono<Void> handleFloorPump() {
        return queryFloorPumpStatus()
            .flatMap(response -> controlFloorPump());
    }

    private Mono<ShellyPro4StatusResponse> queryFloorPumpStatus() {
        return heaterPro4Client.getFloorPumpStatus()
            .doOnNext(response -> {
                floorPump.setRunning(Boolean.TRUE.equals(response.getOutput()));
                log.info("Pump status [FLOOR] isWorking: [{}]", response.getOutput());
            });
    }

    private Mono<Void> controlFloorPump() {
        if (isAnyFloorWorking() && !floorPump.isRunning()) {
            return turnOnFloorPump();
        } else {
            return turnOffFloorPump();
        }
    }

    private boolean isAnyFloorWorking() {
        return Stream.of(wardrobe, bathroomUp).anyMatch(Room::isHeatingActive);
    }

    private Mono<Void> turnOnFloorPump() {
        if (!floorPump.isRunning()) {
            return heaterPro4Client.controlPumpFloor(true)
                .doOnError(throwable -> log.error("Error while turning on floor pump", throwable))
                .doOnNext(response -> {
                    log.info("Floor pump turned on");
                    floorPump.setStartedAt(LocalDateTime.now());
                    floorPump.setRunning(Boolean.TRUE.equals(response.getIson()));
                })
                .then();
        }
        return Mono.empty();
    }

    private Mono<Void> turnOffFloorPump() {
        if (floorPump.isRunning()) {
            return heaterPro4Client.controlPumpFloor(false)
                .doOnError(throwable -> log.error("Error while turning off floor pump", throwable))
                .doOnNext(response -> {
                    log.info("Floor pump turned off");
                    floorPump.setStoppedAt(LocalDateTime.now());
                    floorPump.setRunning(Boolean.TRUE.equals(response.getIson()));
                })
                .then();
        }
        return Mono.empty();
    }
}
