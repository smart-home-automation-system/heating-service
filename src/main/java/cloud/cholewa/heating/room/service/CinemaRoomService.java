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
import static cloud.cholewa.home.model.RoomName.CINEMA;

@Slf4j
@Service
@RequiredArgsConstructor
public class CinemaRoomService {

    private final Room cinema;

    private final ScheduleService scheduleService;
    private final RoomHeatingService roomHeatingService;
    private final HeaterPro4Config heaterPro4Config;
    private final HeaterPro4Client heaterPro4Client;

    public Mono<Void> handleCinemaRoom() {
        Schedule activeSchedule = scheduleService.findSchedule(cinema);
        HeaterActor radiatorActor = roomHeatingService.getRadiatorActor(cinema);

        if (roomHeatingService.shouldTurnOnRadiatorBySchedule(cinema, activeSchedule, radiatorActor)) {
            turnOnCinemaHeater(activeSchedule);
        } else if (roomHeatingService.shouldTurnOnRadiatorByFireplace(cinema, radiatorActor)) {
            turnOnCinemaHeater(null);
        } else if (roomHeatingService.shouldTurnOffHeater(cinema, activeSchedule, radiatorActor)) {
            turnOffCinemaHeater();
        }

        cinema.setHeatingActive(cinema.getHeaterActors().stream().anyMatch(HeaterActor::isWorking));
        return Mono.empty();
    }

    private void turnOnCinemaHeater(final Schedule schedule) {
        heaterPro4Client.controlHeatingActor(heaterPro4Config.getShellyPro4HeaterHost(CINEMA), true)
            .doOnError(throwable -> log.error("Error while turning on cinema heater", throwable))
            .doOnNext(response -> {
                if (schedule != null) {
                    log.info(
                        "[{}] heater turned on due to schedule and temperature less than [{} C]",
                        RoomName.fromValue(cinema.getName()),
                        schedule.getTemperature()
                    );
                } else {
                    log.info(
                        "[{}] heater turned on due to fireplace is working and temperature less than [{} C]",
                        RoomName.fromValue(cinema.getName()),
                        ROOM_MIN_TEMP_TURN_ON_HEATING_BY_FIREPLACE
                    );
                }
            })
            .subscribe();
    }

    private void turnOffCinemaHeater() {
        heaterPro4Client.controlHeatingActor(heaterPro4Config.getShellyPro4HeaterHost(CINEMA), false)
            .doOnError(throwable -> log.error("Error while turning off cinema heater", throwable))
            .doOnNext(response -> {
                log.info(
                    "[{}] heater turned off due no active schedule or fireplace not working",
                    RoomName.fromValue(cinema.getName())
                );
            })
            .subscribe();
    }
}
