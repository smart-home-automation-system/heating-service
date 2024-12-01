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
import static cloud.cholewa.home.model.RoomName.LIVIA;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiviaRoomService {

    private final Room livia;

    private final ScheduleService scheduleService;
    private final RoomHeatingService roomHeatingService;
    private final HeaterPro4Config heaterPro4Config;
    private final HeaterPro4Client heaterPro4Client;

    public Mono<Void> handleLiviaRoom() {
        Schedule activeSchedule = scheduleService.findSchedule(livia);
        HeaterActor radiatorActor = roomHeatingService.getRadiatorActor(livia);

        if (roomHeatingService.shouldTurnOnRadiatorBySchedule(livia, activeSchedule, radiatorActor)) {
            turnOnLiviaHeater(activeSchedule);
        } else if (roomHeatingService.shouldTurnOnRadiatorByFireplace(livia, radiatorActor)) {
            turnOnLiviaHeater(null);
        } else if (roomHeatingService.shouldTurnOffHeater(livia, activeSchedule, radiatorActor)) {
            turnOffLiviaHeater();
        }

        livia.setHeatingActive(livia.getHeaterActors().stream().anyMatch(HeaterActor::isWorking));
        return Mono.empty();
    }

    private void turnOnLiviaHeater(final Schedule schedule) {
        heaterPro4Client.controlHeatingActor(heaterPro4Config.getShellyPro4HeaterHost(LIVIA), true)
            .doOnError(throwable -> log.error("Error while turning on livia heater", throwable))
            .doOnNext(response -> {
                if (schedule != null) {
                    log.info(
                        "[{}] heater turned on due to schedule and temperature less than [{} C]",
                        RoomName.fromValue(livia.getName()),
                        schedule.getTemperature()
                    );
                } else {
                    log.info(
                        "[{}] heater turned on due to fireplace is working and temperature less than [{} C]",
                        RoomName.fromValue(livia.getName()),
                        ROOM_MIN_TEMP_TURN_ON_HEATING_BY_FIREPLACE
                    );
                }
            })
            .subscribe();
    }

    private void turnOffLiviaHeater() {
        heaterPro4Client.controlHeatingActor(heaterPro4Config.getShellyPro4HeaterHost(LIVIA), false)
            .doOnError(throwable -> log.error("Error while turning off livia heater", throwable))
            .doOnNext(response -> {
                log.info(
                    "[{}] heater turned off due no active schedule or fireplace not working",
                    RoomName.fromValue(livia.getName())
                );
            })
            .subscribe();
    }
}
