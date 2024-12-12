package cloud.cholewa.heating.room.service;

import cloud.cholewa.heating.model.AlertReason;
import cloud.cholewa.heating.model.BoilerRoom;
import cloud.cholewa.heating.model.Fireplace;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.heating.model.Room;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static cloud.cholewa.heating.model.HeatingTemperatures.FIREPLACE_TEMPERATURE_ALLOW_ENABLE_HEATER;

@Slf4j
@Service
@RequiredArgsConstructor
class RoomHeatingTools {

    private final BoilerRoom boilerRoom;
    private final Fireplace fireplace;

    Mono<Room> hasAnyHeaterActor(final Room room) {
        return room.getHeaterActors().isEmpty()
            ? Mono.empty()
            : Mono.just(room);
    }

    Optional<HeaterActor> getRadiatorActor(final Room room) {
        return room.getHeaterActors().stream()
            .filter(heaterActor -> heaterActor.getName() == HeaterType.RADIATOR)
            .findFirst();
    }

    Optional<HeaterActor> getFloorActor(final Room room) {
        return room.getHeaterActors().stream()
            .filter(heaterActor -> heaterActor.getName() == HeaterType.FLOOR)
            .findFirst();
    }

    boolean isAlertActive() {
        return boilerRoom.getAlert().getReason() != AlertReason.NO_ALERT;
    }

    boolean isFireplaceActive() {
        return fireplace.temperature().getValue() >= FIREPLACE_TEMPERATURE_ALLOW_ENABLE_HEATER;
    }

    boolean hasTemperatureUnderMin(final Room room) {
        return switch (room.getName()) {
            case OFFICE, TOBI, LIVIA, BEDROOM, CINEMA, LIVING_ROOM, BATHROOM_DOWN, BATHROOM_UP ->
                room.getTemperature().getValue() < 19;
            case WARDROBE -> room.getTemperature().getValue() < 18;
            case ENTRANCE -> room.getTemperature().getValue() < 17;
            case GARAGE -> room.getTemperature().getValue() < 14;
            default -> room.getTemperature().getValue() < 10;
        };
    }
}
