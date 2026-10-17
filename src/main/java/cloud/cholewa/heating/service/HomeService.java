package cloud.cholewa.heating.service;

import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.HomeStatus;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.window.WindowSensors;
import cloud.cholewa.home.model.RoomName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeService {

    private final Clock clock;
    private final Home home;
    private final HomeStatus homeStatus;
    private final ScheduleService scheduleService;
    private final HeatingService heatingService;
    private final FloorPumpService floorPumpService;
    private final WindowSensors windowSensors;

    public Mono<Void> processRoomTemperature(final RoomName roomName, final double temperature) {
        return Flux.fromIterable(home.rooms())
            .filter(room -> room.getName().equals(roomName))
            .doOnNext(room -> updateRoomTemperature(room, temperature))
            .flatMap(scheduleService::processSchedule)
            .onErrorResume(throwable -> {
                log.error("Error processing schedule for room: {}", roomName);
                return Mono.empty();
            })
            .flatMap(heatingService::processHeatingRequest)
            .onErrorResume(throwable -> {
                log.error("Error processing heating request for room: {}: {}", roomName, throwable.toString());
                return Mono.empty();
            })
            .flatMap(this::determineAnyHeaterActive)
            .flatMap(room -> floorPumpService.processFloorPump())
            .then();
    }

    //A room is read only when its temperature changes - in the slowest room every quarter of an hour -
    //so a window that has stood open for hold-after would wait that long for the pass that stops its
    //heaters (HAS-232). Every minute the rooms that are held and still have a heater working get that
    //pass without a reading: the decisions of the last reading stand, only the hold is new. Nothing is
    //switched on here - a held room switches off, and resumes with its next reading after it closes
    public Mono<Void> stopHeldRooms() {
        return Flux.defer(() -> Flux.fromIterable(home.rooms()))
            .filter(room -> room.getHeaterActors().stream().anyMatch(HeaterActor::isWorking))
            .filter(room -> windowSensors.windowOf(room.getName()).hold())
            .concatMap(room -> heatingService.processHeatingRequest(room)
                .onErrorResume(throwable -> {
                    log.error("Error stopping the heating of room: {} held by an open window: {}", room.getName(), throwable.toString());
                    return Mono.empty();
                }))
            .takeLast(1)
            .flatMap(this::determineAnyHeaterActive)
            .flatMap(room -> floorPumpService.processFloorPump())
            .then();
    }

    private void updateRoomTemperature(final Room room, final double temperature) {
        //as one: RoomMapper reads the reading and RoomTemperatureSeeder checks for one under the same lock
        room.getTemperature().update(temperature, LocalDateTime.now(clock));
    }

    private Mono<Room> determineAnyHeaterActive(final Room room) {
        return Mono.defer(() -> {
            home.rooms().stream()
                .map(Room::getHeaterActors)
                .flatMap(List::stream)
                .map(HeaterActor::isWorking)
                .reduce(Boolean::logicalOr)
                .ifPresentOrElse(
                    anyActive -> {
                        homeStatus.setAnyHeaterActive(anyActive);
                        if (anyActive) {
                            log.info("Heater actors are active");
                        } else {
                            log.info("No heater actors are active");
                        }
                    },
                    () -> {
                        log.error("No heater actors found");
                        homeStatus.setAnyHeaterActive(false);
                    }
                );
            return Mono.just(room);
        });
    }
}
