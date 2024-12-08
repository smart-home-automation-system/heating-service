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
import static cloud.cholewa.home.model.RoomName.WARDROBE;

@Slf4j
@Service
@RequiredArgsConstructor
public class WardrobeRoomService {

    private final Room wardrobe;

    private final ScheduleService scheduleService;
    private final RoomHeatingService roomHeatingService;
    private final HeaterPro4Config heaterPro4Config;
    private final HeaterPro4Client heaterPro4Client;

    public Mono<Void> handleWardrobeRoom() {
        Schedule activeSchedule = scheduleService.findSchedule(wardrobe);
        HeaterActor radiatorActor = roomHeatingService.getFloorActor(wardrobe);

        if (roomHeatingService.shouldTurnOnRadiatorBySchedule(wardrobe, activeSchedule, radiatorActor)) {
            turnOnWardrobeHeater(activeSchedule);
        } else if (roomHeatingService.shouldTurnOnRadiatorByFireplace(wardrobe, radiatorActor)) {
            turnOnWardrobeHeater(null);
        } else if (roomHeatingService.shouldTurnOffHeater(wardrobe, activeSchedule, radiatorActor)) {
            turnOffWardrobeHeater();
        }

        wardrobe.setHeatingActive(wardrobe.getHeaterActors().stream().anyMatch(HeaterActor::isWorking));
        return Mono.empty();
    }

    private void turnOnWardrobeHeater(final Schedule schedule) {
        heaterPro4Client.controlHeatingActor(heaterPro4Config.getShellyPro4FloorHost(WARDROBE), true)
            .doOnError(throwable -> log.error("Error while turning on wardrobe floor", throwable))
            .doOnNext(response -> {
                if (schedule != null) {
                    log.info(
                        "[{}] heater turned on due to schedule and temperature less than [{} C]",
                        RoomName.fromValue(wardrobe.getName()),
                        schedule.getTemperature()
                    );
                } else {
                    log.info(
                        "[{}] heater turned on due to fireplace is working and temperature less than [{} C]",
                        RoomName.fromValue(wardrobe.getName()),
                        ROOM_MIN_TEMP_TURN_ON_HEATING_BY_FIREPLACE
                    );
                }
            })
            .subscribe();
    }

    private void turnOffWardrobeHeater() {
        heaterPro4Client.controlHeatingActor(heaterPro4Config.getShellyPro4FloorHost(WARDROBE), false)
            .doOnError(throwable -> log.error("Error while turning off wardrobe heater", throwable))
            .doOnNext(response -> {
                log.info(
                    "[{}] heater turned off due no active schedule or fireplace not working",
                    RoomName.fromValue(wardrobe.getName())
                );
            })
            .subscribe();
    }
}
