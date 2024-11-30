package cloud.cholewa.heating.room.service;

import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.Schedule;
import cloud.cholewa.heating.shelly.actor.HeaterPro4Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static cloud.cholewa.heating.model.HeatingTemperatures.ROOM_MIN_TEMP_TURN_ON_HEATING_BY_FIREPLACE;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfficeRoomService {

    private final Room office;

    private final ScheduleService scheduleService;
    private final RoomHeatingService roomHeatingService;
    private final HeaterPro4Client heaterPro4Client;

    public void handleOfficeRoom() {
        Schedule activeSchedule = scheduleService.findSchedule(office);
        HeaterActor radiatorActor = roomHeatingService.getRadiatorActor(office);

        if (roomHeatingService.shouldTurnOnRadiatorBySchedule(office, activeSchedule, radiatorActor)) {
            turnOnOfficeHeater(activeSchedule);
        } else if (roomHeatingService.shouldTurnOnRadiatorByFireplace(office, radiatorActor)) {
            turnOnOfficeHeater(null);
        } else if (roomHeatingService.shouldTurnOffHeater(office, activeSchedule, radiatorActor)) {
            turnOffOfficeHeater();
        }

        office.setHeatingActive(office.getHeaterActors().stream().anyMatch(HeaterActor::isWorking));
    }

    private void turnOnOfficeHeater(final Schedule schedule) {
        heaterPro4Client.controlOffice(true)
            .doOnError(throwable -> log.error("Error while turning on office heater", throwable))
            .doOnNext(response -> {
                if (schedule != null) {
                    log.info(
                        "Office heater turned on due to schedule and temperature less than [{} C]",
                        schedule.getTemperature()
                    );
                } else {
                    log.info(
                        "Office heater turned on due to fireplace is working and temperature less than [{} C]",
                        ROOM_MIN_TEMP_TURN_ON_HEATING_BY_FIREPLACE
                    );
                }
            })
            .subscribe();
    }

    private void turnOffOfficeHeater() {
        heaterPro4Client.controlOffice(false)
            .doOnError(throwable -> log.error("Error while turning off office heater", throwable))
            .doOnNext(response -> {
                log.info("Office heater turned off due no active schedule or fireplace not working");
            })
            .subscribe();
    }
}
