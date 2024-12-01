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
import static cloud.cholewa.home.model.RoomName.BEDROOM;

@Slf4j
@Service
@RequiredArgsConstructor
public class BedroomRoomService {

    private final Room bedroom;

    private final ScheduleService scheduleService;
    private final RoomHeatingService roomHeatingService;
    private final HeaterPro4Config heaterPro4Config;
    private final HeaterPro4Client heaterPro4Client;

    public Mono<Void> handleBedroomRoom() {
        Schedule activeSchedule = scheduleService.findSchedule(bedroom);
        HeaterActor radiatorActor = roomHeatingService.getRadiatorActor(bedroom);

        if (roomHeatingService.shouldTurnOnRadiatorBySchedule(bedroom, activeSchedule, radiatorActor)) {
            turnOnBedroomHeater(activeSchedule);
        } else if (roomHeatingService.shouldTurnOnRadiatorByFireplace(bedroom, radiatorActor)) {
            turnOnBedroomHeater(null);
        } else if (roomHeatingService.shouldTurnOffHeater(bedroom, activeSchedule, radiatorActor)) {
            turnOffBedroomHeater();
        }

        bedroom.setHeatingActive(bedroom.getHeaterActors().stream().anyMatch(HeaterActor::isWorking));
        return Mono.empty();
    }

    private void turnOnBedroomHeater(final Schedule schedule) {
        heaterPro4Client.controlHeatingActor(heaterPro4Config.getShellyPro4HeaterHost(BEDROOM), true)
            .doOnError(throwable -> log.error("Error while turning on bedroom heater", throwable))
            .doOnNext(response -> {
                if (schedule != null) {
                    log.info(
                        "[{}] heater turned on due to schedule and temperature less than [{} C]",
                        RoomName.fromValue(bedroom.getName()),
                        schedule.getTemperature()
                    );
                } else {
                    log.info(
                        "[{}] heater turned on due to fireplace is working and temperature less than [{} C]",
                        RoomName.fromValue(bedroom.getName()),
                        ROOM_MIN_TEMP_TURN_ON_HEATING_BY_FIREPLACE
                    );
                }
            })
            .subscribe();
    }

    private void turnOffBedroomHeater() {
        heaterPro4Client.controlHeatingActor(heaterPro4Config.getShellyPro4HeaterHost(BEDROOM), false)
            .doOnError(throwable -> log.error("Error while turning off bedroom heater", throwable))
            .doOnNext(response -> {
                log.info(
                    "[{}] heater turned off due no active schedule or fireplace not working",
                    RoomName.fromValue(bedroom.getName())
                );
            })
            .subscribe();
    }
}
