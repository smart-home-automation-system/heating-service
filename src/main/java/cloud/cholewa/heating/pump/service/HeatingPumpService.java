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

    public void handleHeatingPump() {
        if (boilerRoom.isHeatingEnabled()) {
            if (isAnyRoomHeatingActive() && isFireplaceNotActive() && !hotWaterPump.isRunning()) {
                if (!heatingPump.isRunning()) {
                    boilerPro4Client.controlHeatingPump(true)
                        .doOnError(throwable -> log.error("Error while turning on heating pump", throwable))
                        .doOnNext(response -> {
                            log.info("Heating pump turned on - some rooms are ready to heat");
                            heatingPump.setStartedAt(LocalDateTime.now());
                        })
                        .subscribe();
                }
            } else {
                if (heatingPump.isRunning()) {
                    boilerPro4Client.controlHeatingPump(false)
                        .doOnError(throwable -> log.error("Error while turning off heating pump", throwable))
                        .doOnNext(response -> {
                            log.info("Heating pump turned off - no rooms to heat");
                            heatingPump.setStoppedAt(LocalDateTime.now());
                        })
                        .subscribe();
                }
            }
        } else {
            if (heatingPump.isRunning()) {
                boilerPro4Client.controlHeatingPump(false)
                    .doOnError(throwable -> log.error("Error while turning off heating pump", throwable))
                    .doOnNext(response -> {
                        log.info("Heating pump turned off - heating is disabled");
                        heatingPump.setStoppedAt(LocalDateTime.now());
                    })
                    .subscribe();
            }
        }
    }

    private boolean isAnyRoomHeatingActive() {
        return rooms.stream().anyMatch(Room::isHeatingActive);
    }

    private boolean isFireplaceNotActive() {
        return fireplace.temperature().getValue() < HeatingTemperatures.FIREPLACE_TEMPERATURE_VALID_TO_ENABLE_FURNACE;
    }
}
