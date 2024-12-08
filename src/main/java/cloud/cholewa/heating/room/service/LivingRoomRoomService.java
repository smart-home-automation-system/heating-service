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

import static cloud.cholewa.heating.model.HeatingTemperatures.ROOM_MIN_TEMP_TURN_ON_HEATING_BY_FIREPLACE;
import static cloud.cholewa.home.model.RoomName.KITCHEN;
import static cloud.cholewa.home.model.RoomName.LIVING_ROOM;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivingRoomRoomService {

    private final Room livingRoom;

    private final ScheduleService scheduleService;
    private final RoomHeatingService roomHeatingService;
    private final HeaterPro4Config heaterPro4Config;
    private final HeaterPro4Client heaterPro4Client;

    public Mono<Void> handleLivingRoom() {
        Schedule activeSchedule = scheduleService.findSchedule(livingRoom);
        HeaterActor radiatorActor = roomHeatingService.getRadiatorActor(livingRoom);
//        HeaterActor floorActor = roomHeatingService.getFloorActor(livingRoom);

        if (roomHeatingService.shouldTurnOnRadiatorBySchedule(livingRoom, activeSchedule, radiatorActor)) {
            turnOnLivingRoomHeater(activeSchedule);
            turnOnLivingRoomFloor(activeSchedule);
        } else if (roomHeatingService.shouldTurnOnRadiatorByFireplace(livingRoom, radiatorActor)) {
            turnOnLivingRoomHeater(null);
            turnOnLivingRoomFloor(null);
        } else if (roomHeatingService.shouldTurnOffHeater(livingRoom, activeSchedule, radiatorActor)) {
            turnOffLivingRoomHeater();
            turnOffLivingRoomFloor();
        }

        livingRoom.setHeatingActive(livingRoom.getHeaterActors().stream().anyMatch(HeaterActor::isWorking));
        return Mono.empty();
    }

    private void turnOnLivingRoomHeater(final Schedule schedule) {
        heaterPro4Client.controlHeatingActor(heaterPro4Config.getShellyPro4HeaterHost(LIVING_ROOM), true)
            .doOnError(throwable -> log.error("Error while turning on living room heater", throwable))
            .doOnNext(response -> {
                if (schedule != null) {
                    log.info(
                        "[{}] heater turned on due to schedule and temperature less than [{} C]",
                        RoomName.fromValue(livingRoom.getName().replace("_", "")),
                        schedule.getTemperature()
                    );
                } else {
                    log.info(
                        "[{}] heater turned on due to fireplace is working and temperature less than [{} C]",
                        RoomName.fromValue(livingRoom.getName().replace("_", "")),
                        ROOM_MIN_TEMP_TURN_ON_HEATING_BY_FIREPLACE
                    );
                }
            })
            .subscribe();
    }

    private void turnOffLivingRoomHeater() {
        heaterPro4Client.controlHeatingActor(heaterPro4Config.getShellyPro4HeaterHost(LIVING_ROOM), false)
            .doOnError(throwable -> log.error("Error while turning off living room heater", throwable))
            .doOnNext(response -> {
                log.info(
                    "[{}] heater turned off due no active schedule or fireplace not working",
                    RoomName.fromValue(livingRoom.getName().replace("_", ""))
                );
            })
            .subscribe();
    }

    private void turnOnLivingRoomFloor(final Schedule schedule) {
        heaterPro4Client.controlHeatingActor(heaterPro4Config.getShellyPro4FloorHost(KITCHEN), true)
            .doOnError(throwable -> log.error("Error while turning on living room heater", throwable))
            .doOnNext(response -> {
                if (schedule != null) {
                    log.info(
                        "[{}] floor turned on due to schedule and temperature less than [{} C]",
                        RoomName.fromValue(livingRoom.getName().replace("_", "")),
                        schedule.getTemperature()
                    );
                } else {
                    log.info(
                        "[{}] floor turned on due to fireplace is working and temperature less than [{} C]",
                        RoomName.fromValue(livingRoom.getName().replace("_", "")),
                        ROOM_MIN_TEMP_TURN_ON_HEATING_BY_FIREPLACE
                    );
                }
            })
            .subscribe();
    }

    private void turnOffLivingRoomFloor() {
        heaterPro4Client.controlHeatingActor(heaterPro4Config.getShellyPro4FloorHost(KITCHEN), false)
            .doOnError(throwable -> log.error("Error while turning off living room floor", throwable))
            .doOnNext(response -> {
                log.info(
                    "[{}] floor turned off due no active schedule or fireplace not working",
                    RoomName.fromValue(livingRoom.getName().replace("_", ""))
                );
            })
            .subscribe();
    }
}
