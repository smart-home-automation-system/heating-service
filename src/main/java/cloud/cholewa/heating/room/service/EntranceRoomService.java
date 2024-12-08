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
import static cloud.cholewa.home.model.RoomName.ENTRANCE;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntranceRoomService {

    private final Room entrance;

    private final ScheduleService scheduleService;
    private final RoomHeatingService roomHeatingService;
    private final HeaterPro4Config heaterPro4Config;
    private final HeaterPro4Client heaterPro4Client;

    public Mono<Void> handleEntranceRoom() {
        Schedule activeSchedule = scheduleService.findSchedule(entrance);
        HeaterActor radiatorActor = roomHeatingService.getRadiatorActor(entrance);

        if (roomHeatingService.shouldTurnOnRadiatorBySchedule(entrance, activeSchedule, radiatorActor)) {
            turnOnEntranceHeater(activeSchedule);
//        } else if (roomHeatingService.shouldTurnOnRadiatorByFireplace(entrance, radiatorActor)) {
//            turnOnEntranceHeater(null);
        } else if (roomHeatingService.shouldTurnOffHeater(entrance, activeSchedule, radiatorActor)) {
            turnOffEntranceHeater();
        }

        entrance.setHeatingActive(entrance.getHeaterActors().stream().anyMatch(HeaterActor::isWorking));
        return Mono.empty();
    }

    private void turnOnEntranceHeater(final Schedule schedule) {
        heaterPro4Client.controlHeatingActor(heaterPro4Config.getShellyPro4HeaterHost(ENTRANCE), true)
            .doOnError(throwable -> log.error("Error while turning on ENTRANCE heater", throwable))
            .doOnNext(response -> {
                if (schedule != null) {
                    log.info(
                        "[{}] heater turned on due to schedule and temperature less than [{} C]",
                        RoomName.fromValue(entrance.getName()),
                        schedule.getTemperature()
                    );
                } else {
                    log.info(
                        "[{}] heater turned on due to fireplace is working and temperature less than [{} C]",
                        RoomName.fromValue(entrance.getName()),
                        ROOM_MIN_TEMP_TURN_ON_HEATING_BY_FIREPLACE
                    );
                }
            })
            .subscribe();
    }

    private void turnOffEntranceHeater() {
        heaterPro4Client.controlHeatingActor(heaterPro4Config.getShellyPro4HeaterHost(ENTRANCE), false)
            .doOnError(throwable -> log.error("Error while turning off ENTRANCE heater", throwable))
            .doOnNext(response -> {
                log.info(
                    "[{}] heater turned off due no active schedule or fireplace not working",
                    RoomName.fromValue(entrance.getName())
                );
            })
            .subscribe();
    }
}
