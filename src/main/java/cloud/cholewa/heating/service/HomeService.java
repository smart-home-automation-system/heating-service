package cloud.cholewa.heating.service;

import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.HomeStatus;
import cloud.cholewa.heating.model.Room;
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
            .flatMap(room -> determineAnyHeaterActive().thenReturn(room))
            .flatMap(room -> floorPumpService.processFloorPump())
            .then();
    }

    //HAS-352: every room decided without a reading - the path of a reading without its temperature. A room
    //is read only when its temperature changes (in the slowest room every quarter of an hour), so the switch
    //of the house, a boundary of a schedule and an open window waited for the next reading of each room: on
    //2026-10-30 two rooms started nine minutes after the switch. Run every minute (DecisionScheduler) and
    //right after the switch (HeatingStatusService). The schedule and the decision of every room with a known
    //temperature - a seeded one too, the last stored reading - then "any heater active" and the floor pump
    //once. A room without a temperature yet is skipped; a room that fails is logged and the others go on. A
    //relay is commanded only when its state differs and a status read only when older than five minutes
    //(HeatingService), so a quiet minute calls no device. The rooms one after the other, as readings come
    public Mono<Void> decideRooms() {
        return Flux.defer(() -> Flux.fromIterable(home.rooms()))
            .filter(room -> room.getTemperature() != null && room.getTemperature().snapshot().getUpdatedAt() != null)
            .concatMap(room -> scheduleService.processSchedule(room)
                .flatMap(heatingService::processHeatingRequest)
                .onErrorResume(throwable -> {
                    log.error("Error deciding the heating of room: {}: {}", room.getName(), throwable.toString());
                    return Mono.empty();
                }))
            .then(Mono.defer(this::determineAnyHeaterActive))
            .then(Mono.defer(floorPumpService::processFloorPump));
    }

    private void updateRoomTemperature(final Room room, final double temperature) {
        //as one: RoomMapper reads the reading and RoomTemperatureSeeder checks for one under the same lock
        room.getTemperature().update(temperature, LocalDateTime.now(clock));
    }

    //logged on a change only: the decision of every minute (HAS-352) would otherwise log a line a minute
    private Mono<Void> determineAnyHeaterActive() {
        return Mono.fromRunnable(() -> home.rooms().stream()
            .map(Room::getHeaterActors)
            .flatMap(List::stream)
            .map(HeaterActor::isWorking)
            .reduce(Boolean::logicalOr)
            .ifPresentOrElse(
                anyActive -> {
                    if (anyActive != homeStatus.isAnyHeaterActive()) {
                        log.info(Boolean.TRUE.equals(anyActive) ? "Heater actors are active" : "No heater actors are active");
                    }
                    homeStatus.setAnyHeaterActive(anyActive);
                },
                () -> {
                    log.error("No heater actors found");
                    homeStatus.setAnyHeaterActive(false);
                }
            ));
    }
}
