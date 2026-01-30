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
                log.error("Error processing heating request for room: {}", roomName);
                return Mono.empty();
            })
            .flatMap(this::determineAnyHeaterActive)
            .flatMap(room -> floorPumpService.processFloorPump())
            .then();
    }

    private void updateRoomTemperature(final Room room, final double temperature) {
        room.getTemperature().setUpdatedAt(LocalDateTime.now(clock));
        room.getTemperature().setValue(temperature);
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
