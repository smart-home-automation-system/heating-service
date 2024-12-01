package cloud.cholewa.heating.pump.service;

import cloud.cholewa.heating.model.Pump;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.shelly.actor.HeaterPro4Client;
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
        if (isAnyFloorWorking()) {
            turnOnFloorPump();
        } else {
            turnOffFloorPump();
        }
        return Mono.empty();
    }

    private boolean isAnyFloorWorking() {
        return Stream.of(wardrobe, bathroomUp)
            .map(Room::isHeatingActive)
            .anyMatch(aBoolean -> aBoolean.equals(true));
    }

    private void turnOnFloorPump() {
        if (!floorPump.isRunning()) {
            heaterPro4Client.controlPumpFloor(true)
                .doOnError(throwable -> log.error("Error while turning on floor pump", throwable))
                .doOnNext(response -> {
                    log.info("Floor pump turned on");
                    floorPump.setStartedAt(LocalDateTime.now());
                })
                .subscribe();
        }
    }

    private void turnOffFloorPump() {
        if (floorPump.isRunning()) {
            heaterPro4Client.controlPumpFloor(false)
                .doOnError(throwable -> log.error("Error while turning off floor pump", throwable))
                .doOnNext(response -> {
                    log.info("Floor pump turned off");
                    floorPump.setStoppedAt(LocalDateTime.now());
                })
                .subscribe();
        }
    }
}
