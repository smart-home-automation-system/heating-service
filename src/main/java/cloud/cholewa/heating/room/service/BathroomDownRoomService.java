package cloud.cholewa.heating.room.service;

import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.Schedule;
import cloud.cholewa.heating.shelly.actor.HeaterPro4Client;
import cloud.cholewa.heating.shelly.actor.HeaterPro4Config;
import cloud.cholewa.home.model.RoomName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalTime;

import static cloud.cholewa.heating.model.HeatingTemperatures.ROOM_MIN_TEMP_TURN_ON_HEATING_BY_FIREPLACE;
import static cloud.cholewa.home.model.RoomName.BATHROOM_DOWN;

@Slf4j
@Service
@RequiredArgsConstructor
public class BathroomDownRoomService {

    private final Room bathroomDown;

    private final ScheduleService scheduleService;
    private final RoomHeatingService roomHeatingService;
    private final HeaterPro4Config heaterPro4Config;
    private final HeaterPro4Client heaterPro4Client;

    public Mono<Void> handleBathroomDownRoom() {
        Schedule activeSchedule = scheduleService.findSchedule(bathroomDown);
        HeaterActor floorActor = roomHeatingService.getFloorActor(bathroomDown);
        HeaterActor radiatorActor = roomHeatingService.getRadiatorActor(bathroomDown);

        if (LocalTime.now().isAfter(LocalTime.of(16, 0)) && LocalTime.now().isBefore(LocalTime.of(19, 0))) {
            if (!floorActor.isWorking()) {
                turnOnBathroomDownFloor(null);
                turnOnBathroomDownHeater(null);
            }
        } else {
            if (floorActor.isWorking()) {
                turnOffBathroomDownFloor();
                turnOffBathroomDownHeater();
            }
        }

        if (roomHeatingService.shouldTurnOnRadiatorBySchedule(bathroomDown, activeSchedule, radiatorActor)) {
            turnOnBathroomDownFloor(activeSchedule);
        } else if (roomHeatingService.shouldTurnOnRadiatorByFireplace(bathroomDown, radiatorActor)) {
            turnOnBathroomDownFloor(null);
        } else if (roomHeatingService.shouldTurnOffHeater(bathroomDown, activeSchedule, radiatorActor)) {
            turnOffBathroomDownFloor();
        }

        bathroomDown.setHeatingActive(bathroomDown.getHeaterActors().stream().anyMatch(HeaterActor::isWorking));
        return Mono.empty();
    }

    private void turnOnBathroomDownHeater(final Schedule schedule) {
        heaterPro4Client.controlHeatingActor(heaterPro4Config.getShellyPro4HeaterHost(BATHROOM_DOWN), true)
            .doOnError(throwable -> log.error("Error while turning on bathroom down heater", throwable))
            .doOnNext(response -> {
                if (schedule != null) {
                    log.info(
                        "[{}] heater turned on due to schedule and temperature less than [{} C]",
                        RoomName.fromValue(bathroomDown.getName().replace("_", "")),
                        schedule.getTemperature()
                    );
                } else {
                    log.info(
                        "[{}] heater turned on due to fireplace is working and temperature less than [{} C]",
                        RoomName.fromValue(bathroomDown.getName().replace("_", "")),
                        ROOM_MIN_TEMP_TURN_ON_HEATING_BY_FIREPLACE
                    );
                }
            })
            .subscribe();
    }

    private void turnOnBathroomDownFloor(final Schedule schedule) {
        heaterPro4Client.controlHeatingActor(heaterPro4Config.getShellyPro4FloorHost(BATHROOM_DOWN), true)
            .doOnError(throwable -> log.error("Error while turning on bathroom down floor", throwable))
            .doOnNext(response -> {
                if (schedule != null) {
                    log.info(
                        "[{}] floor turned on due to schedule and temperature less than [{} C]",
                        RoomName.fromValue(bathroomDown.getName().replace("_", "")),
                        schedule.getTemperature()
                    );
                } else {
                    log.info(
                        "[{}] floor turned on due to fireplace is working and temperature less than [{} C]",
                        RoomName.fromValue(bathroomDown.getName().replace("_", "")),
                        ROOM_MIN_TEMP_TURN_ON_HEATING_BY_FIREPLACE
                    );
                }
            })
            .subscribe();
    }

    private void turnOffBathroomDownHeater() {
        heaterPro4Client.controlHeatingActor(heaterPro4Config.getShellyPro4HeaterHost(BATHROOM_DOWN), false)
            .doOnError(throwable -> log.error("Error while turning off bathroom down heater", throwable))
            .doOnNext(response -> {
                log.info(
                    "[{}] heater turned off due no active schedule or fireplace not working",
                    RoomName.fromValue(bathroomDown.getName().replace("_", ""))
                );
            })
            .subscribe();
    }

    private void turnOffBathroomDownFloor() {
        heaterPro4Client.controlHeatingActor(heaterPro4Config.getShellyPro4FloorHost(BATHROOM_DOWN), false)
            .doOnError(throwable -> log.error("Error while turning off bathroom up floor", throwable))
            .doOnNext(response -> {
                log.info(
                    "[{}] floor turned off due no active schedule or fireplace not working",
                    RoomName.fromValue(bathroomDown.getName().replace("_", ""))
                );
            })
            .subscribe();
    }
}
