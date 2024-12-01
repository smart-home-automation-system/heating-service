package cloud.cholewa.heating.room.service;

import cloud.cholewa.heating.model.BoilerRoom;
import cloud.cholewa.heating.model.Fireplace;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.Schedule;
import cloud.cholewa.heating.model.Temperature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static cloud.cholewa.heating.model.HeatingTemperatures.FIREPLACE_ALERT_TEMPERATURE;
import static cloud.cholewa.heating.model.HeatingTemperatures.FIREPLACE_TEMPERATURE_ALLOW_DISABLE_HEATER;
import static cloud.cholewa.heating.model.HeatingTemperatures.FIREPLACE_TEMPERATURE_ALLOW_ENABLE_HEATER;
import static cloud.cholewa.heating.model.HeatingTemperatures.ROOM_MIN_TEMP_TURN_ON_HEATING_BY_FIREPLACE;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomHeatingService {

    private final BoilerRoom boilerRoom;
    private final Fireplace fireplace;

    public HeaterActor getRadiatorActor(final Room room) {
        return room.getHeaterActors().stream()
            .filter(heaterActor -> heaterActor.getName() == HeaterType.RADIATOR)
            .findFirst().orElseThrow();
    }

    public HeaterActor getFloorActor(final Room room) {
        return room.getHeaterActors().stream()
            .filter(heaterActor -> heaterActor.getName() == HeaterType.FLOOR)
            .findFirst().orElseThrow();
    }

    public boolean shouldTurnOnHeaterByFireplaceAlert(final HeaterActor heaterActor) {
        return fireplace.temperature().getValue() >= FIREPLACE_ALERT_TEMPERATURE
            && !heaterActor.isWorking();
    }

    public boolean shouldTurnOnRadiatorBySchedule(final Room room, final Schedule schedule, final HeaterActor heaterActor) {
        return schedule != null
            && boilerRoom.isHeatingEnabled()
            && !isFireplaceActive()
            && isRoomTemperatureUpdated(room)
            && schedule.getTemperature() >= room.getTemperature().getValue()
            && !heaterActor.isWorking();
    }

    public boolean shouldTurnOnRadiatorByFireplace(final Room room, final HeaterActor heaterActor) {
        return isFireplaceActive()
            && isRoomTemperatureUpdated(room)
            && isRoomTemperatureBelowThreshold(room.getTemperature())
            && !heaterActor.isWorking();
    }

    public boolean shouldTurnOffHeater(final Room room, final Schedule schedule, final HeaterActor heaterActor) {
        return fireplace.temperature().getValue() <= FIREPLACE_TEMPERATURE_ALLOW_DISABLE_HEATER
            && (schedule == null || schedule.getTemperature() < room.getTemperature().getValue() || !boilerRoom.isHeatingEnabled())
            && heaterActor.isWorking();
    }

    private boolean isFireplaceActive() {
        return fireplace.temperature().getValue() >= FIREPLACE_TEMPERATURE_ALLOW_ENABLE_HEATER;
    }

    private boolean isRoomTemperatureBelowThreshold(final Temperature temperature) {
        return temperature.getValue() < ROOM_MIN_TEMP_TURN_ON_HEATING_BY_FIREPLACE;
    }

    private boolean isRoomTemperatureUpdated(final Room room) {
        return room.getTemperature().getUpdatedAt() != null;
    }
}
