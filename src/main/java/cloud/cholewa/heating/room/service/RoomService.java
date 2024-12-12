package cloud.cholewa.heating.room.service;

import cloud.cholewa.heating.model.BoilerRoom;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.pump.service.HeatingPumpService;
import cloud.cholewa.heating.shelly.actor.HeaterPro4Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static cloud.cholewa.heating.model.HeaterType.FLOOR;
import static cloud.cholewa.heating.model.HeaterType.RADIATOR;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final BoilerRoom boilerRoom;

    private final RoomHeatingTools tools;
    private final HeaterActorHandler heaterActorHandler;
    private final HeaterPro4Config heaterPro4Config;
    private final ScheduleService scheduleService;
    private final HeatingPumpService heatingPumpService;

    public Mono<Void> handleRoom(final Room roomToHandle) {
        return tools.hasAnyHeaterActor(roomToHandle)
            .flatMap(this::handleHeaterActors)
            .flatMap(isWorking -> setHeatingActive(roomToHandle))
            .flatMap(is -> heatingPumpService.handleHeatingPump().then(Mono.just(true)))
            .switchIfEmpty(Mono.fromRunnable(() -> log.info(
                "Room: [{}] does not contains any heater actors",
                roomToHandle.getName().name()
            )))
            .then();
    }

    private Mono<Boolean> handleHeaterActors(final Room room) {
        if (tools.getRadiatorActor(room).getName().equals(RADIATOR)) {
            return handleRadiator(room, tools.getRadiatorActor(room));
        } else if (tools.getFloorActor(room).getName().equals(FLOOR)) {
            return handleFloor(room, tools.getFloorActor(room));
        } else {
            return Mono.empty();
        }
    }

    private Mono<Boolean> handleRadiator(final Room room, final HeaterActor radiatorActor) {
        if (tools.isAlertActive()) {
            return heaterActorHandler.turnOnHeaterActor(
                    room,
                    radiatorActor,
                    heaterPro4Config.getShellyPro4HeaterHost(room.getName()),
                    "fireplace HIGH temperature"
                )
                .then(Mono.just(radiatorActor.isWorking()));
        } else if (tools.isFireplaceActive()) { //TODO add temp check
            return heaterActorHandler.turnOnHeaterActor(
                    room,
                    radiatorActor,
                    heaterPro4Config.getShellyPro4HeaterHost(room.getName()),
                    "fireplace is working"
                )
                .then(Mono.just(radiatorActor.isWorking()));
        } else if (scheduleService.hasActiveSchedule(room) && boilerRoom.isHeatingEnabled()) {
            return heaterActorHandler.turnOnHeaterActor(
                    room,
                    radiatorActor,
                    heaterPro4Config.getShellyPro4HeaterHost(room.getName()),
                    "activated by schedule"
                )
                .then(Mono.just(radiatorActor.isWorking()));
        } else if (tools.hasTemperatureUnderMin(room) && boilerRoom.isHeatingEnabled()) {
            return heaterActorHandler.turnOnHeaterActor(
                    room,
                    radiatorActor,
                    heaterPro4Config.getShellyPro4HeaterHost(room.getName()),
                    "temperature is under min value"
                )
                .then(Mono.just(radiatorActor.isWorking()));
        } else {
            return heaterActorHandler.turnOffHeaterActor(
                    room,
                    radiatorActor,
                    heaterPro4Config.getShellyPro4HeaterHost(room.getName())
                )
                .then(Mono.just(radiatorActor.isWorking()));
        }
    }

    private Mono<Boolean> handleFloor(final Room room, final HeaterActor floorActor) {
        return Mono.just(floorActor.isWorking());
    }

    private Mono<Boolean> setHeatingActive(final Room room) {
        room.setHeatingActive(room.getHeaterActors().stream().anyMatch(HeaterActor::isWorking));
        return Mono.just(room.isHeatingActive());
    }
}
