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
import static cloud.cholewa.home.model.RoomName.GARAGE;

@Slf4j
@Service
@RequiredArgsConstructor
public class GarageRoomService {

    private final Room garage;

    private final ScheduleService scheduleService;
    private final RoomHeatingService roomHeatingService;
    private final HeaterPro4Config heaterPro4Config;
    private final HeaterPro4Client heaterPro4Client;

    public Mono<Void> handleGarageRoom() {
        Schedule activeSchedule = scheduleService.findSchedule(garage);
        HeaterActor radiatorActor = roomHeatingService.getRadiatorActor(garage);

        if (roomHeatingService.shouldTurnOnRadiatorBySchedule(garage, activeSchedule, radiatorActor)) {
            turnOnGarageHeater(activeSchedule);
//        } else if (roomHeatingService.shouldTurnOnRadiatorByFireplace(garage, radiatorActor)) {
//            turnOnGarageHeater(null);
        } else if (roomHeatingService.shouldTurnOffHeater(garage, activeSchedule, radiatorActor)) {
            turnOffGarageHeater();
        }

        garage.setHeatingActive(garage.getHeaterActors().stream().anyMatch(HeaterActor::isWorking));
        return Mono.empty();
    }

    private void turnOnGarageHeater(final Schedule schedule) {
        heaterPro4Client.controlHeatingActor(heaterPro4Config.getShellyPro4HeaterHost(GARAGE), true)
            .doOnError(throwable -> log.error("Error while turning on GARAGE heater", throwable))
            .doOnNext(response -> {
                if (schedule != null) {
                    log.info(
                        "[{}] heater turned on due to schedule and temperature less than [{} C]",
                        RoomName.fromValue(garage.getName()),
                        schedule.getTemperature()
                    );
                } else {
                    log.info(
                        "[{}] heater turned on due to fireplace is working and temperature less than [{} C]",
                        RoomName.fromValue(garage.getName()),
                        ROOM_MIN_TEMP_TURN_ON_HEATING_BY_FIREPLACE
                    );
                }
            })
            .subscribe();
    }

    private void turnOffGarageHeater() {
        heaterPro4Client.controlHeatingActor(heaterPro4Config.getShellyPro4HeaterHost(GARAGE), false)
            .doOnError(throwable -> log.error("Error while turning off GARAGE heater", throwable))
            .doOnNext(response -> {
                log.info(
                    "[{}] heater turned off due no active schedule or fireplace not working",
                    RoomName.fromValue(garage.getName())
                );
            })
            .subscribe();
    }
}
