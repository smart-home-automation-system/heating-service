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
import static cloud.cholewa.home.model.RoomName.TOBI;

@Slf4j
@Service
@RequiredArgsConstructor
public class TobiRoomService {

    private final Room tobi;

    private final ScheduleService scheduleService;
    private final RoomHeatingService roomHeatingService;
    private final HeaterPro4Config heaterPro4Config;
    private final HeaterPro4Client heaterPro4Client;

    public Mono<Void> handleTobiRoom() {
        Schedule activeSchedule = scheduleService.findSchedule(tobi);
        HeaterActor radiatorActor = roomHeatingService.getRadiatorActor(tobi);

        if (roomHeatingService.shouldTurnOnRadiatorBySchedule(tobi, activeSchedule, radiatorActor)) {
            turnOnTobiHeater(activeSchedule);
        } else if (roomHeatingService.shouldTurnOnRadiatorByFireplace(tobi, radiatorActor)) {
            turnOnTobiHeater(null);
        } else if (roomHeatingService.shouldTurnOffHeater(tobi, activeSchedule, radiatorActor)) {
            turnOffTobiHeater();
        }

        tobi.setHeatingActive(tobi.getHeaterActors().stream().anyMatch(HeaterActor::isWorking));
        return Mono.empty();
    }

    private void turnOnTobiHeater(final Schedule schedule) {
        heaterPro4Client.controlHeatingActor(heaterPro4Config.getShellyPro4HeaterHost(TOBI), true)
            .doOnError(throwable -> log.error("Error while turning on tobi heater", throwable))
            .doOnNext(response -> {
                if (schedule != null) {
                    log.info(
                        "[{}] heater turned on due to schedule and temperature less than [{} C]",
                        RoomName.fromValue(tobi.getName()),
                        schedule.getTemperature()
                    );
                } else {
                    log.info(
                        "[{}] heater turned on due to fireplace is working and temperature less than [{} C]",
                        RoomName.fromValue(tobi.getName()),
                        ROOM_MIN_TEMP_TURN_ON_HEATING_BY_FIREPLACE
                    );
                }
            })
            .subscribe();
    }

    private void turnOffTobiHeater() {
        heaterPro4Client.controlHeatingActor(heaterPro4Config.getShellyPro4HeaterHost(TOBI), false)
            .doOnError(throwable -> log.error("Error while turning off tobi heater", throwable))
            .doOnNext(response -> {
                log.info(
                    "[{}] heater turned off due no active schedule or fireplace not working",
                    RoomName.fromValue(tobi.getName())
                );
            })
            .subscribe();
    }
}
