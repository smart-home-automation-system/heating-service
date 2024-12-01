package cloud.cholewa.heating.pump.service;

import cloud.cholewa.heating.model.BoilerRoom;
import cloud.cholewa.heating.model.Fireplace;
import cloud.cholewa.heating.model.HeatingTemperatures;
import cloud.cholewa.heating.model.Pump;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.shelly.actor.BoilerPro4Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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

    public Mono<Void> handleHeatingPump() {
        log.info("handleHeatingPump: heatingStatus: [{}]", boilerRoom.isHeatingEnabled());
        if (boilerRoom.isHeatingEnabled()) {
            if (isAnyRoomHeatingActive() && isFireplaceNotActive() && !hotWaterPump.isRunning()) {
                turnOnHeatingPump();
            } else {
                turnOffHeatingPump("no rooms to heat");
            }
        } else {
            turnOffHeatingPump("heating is disabled");
        }
        return Mono.empty();
    }

    private boolean isAnyRoomHeatingActive() {
        log.info("isAnyRoomHeatingActive: [{}]", rooms.stream().anyMatch(Room::isHeatingActive));
        return rooms.stream().anyMatch(Room::isHeatingActive);
    }

    private boolean isFireplaceNotActive() {
        log.info(
            "isFireplaceNotActive: [{}]",
            fireplace.temperature().getValue() < HeatingTemperatures.FIREPLACE_TEMPERATURE_VALID_TO_ENABLE_FURNACE
        );
        return fireplace.temperature().getValue() < HeatingTemperatures.FIREPLACE_TEMPERATURE_VALID_TO_ENABLE_FURNACE;
    }

    private void turnOnHeatingPump() {
        if (!heatingPump.isRunning()) {
            boilerPro4Client.controlHeatingPump(true)
                .doOnError(throwable -> log.error("Error while turning on heating pump", throwable))
                .doOnNext(response -> {
                    log.info("Heating pump turned on - some rooms are ready to heat");
                    heatingPump.setStartedAt(LocalDateTime.now());
                })
                .subscribe();
        }
    }

    private void turnOffHeatingPump(final String messageReason) {
        if (heatingPump.isRunning()) {
            boilerPro4Client.controlHeatingPump(false)
                .doOnError(throwable -> log.error("Error while turning off heating pump", throwable))
                .doOnNext(response -> {
                    log.info("Heating pump turned off - {}", messageReason);
                    heatingPump.setStoppedAt(LocalDateTime.now());
                })
                .subscribe();
        }
    }
}
