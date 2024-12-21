package cloud.cholewa.heating.room.service;

import cloud.cholewa.heating.model.AlertReason;
import cloud.cholewa.heating.model.BoilerRoom;
import cloud.cholewa.heating.model.Fireplace;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.home.model.RoomName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static cloud.cholewa.heating.model.HeatingTemperatures.FIREPLACE_START_TEMPERATURE;

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

    boolean isFireplaceActive(final Room room) {
        return fireplace.temperature().getValue() >= FIREPLACE_START_TEMPERATURE
            && room.getTemperature().getValue() < 20.5
            && (!room.getName().equals(RoomName.ENTRANCE) && !room.getName().equals(RoomName.GARAGE));
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
