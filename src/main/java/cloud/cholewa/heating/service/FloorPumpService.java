package cloud.cholewa.heating.service;

import cloud.cholewa.heating.client.ShellyClient;
import cloud.cholewa.heating.model.FloorPump;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.shelly.model.ShellyPro4StatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDateTime;

import static cloud.cholewa.home.model.RoomName.BATHROOM_UP;
import static cloud.cholewa.home.model.RoomName.WARDROBE;

@Slf4j
@Service
@RequiredArgsConstructor
public class FloorPumpService {

    private final FloorPump floorPump = new FloorPump();

    private final Clock clock;
    private final Home home;
    private final ShellyClient shellyClient;

    public Mono<Void> processFloorPump() {
        return Mono.fromSupplier(this::isStale)
            .filter(Boolean::booleanValue)
            .flatMap(refreshNeeded -> shellyClient.getFloorPumpStatus())
            .doOnError(throwable -> log.error("Error fetching floor pump status: {}", throwable.getMessage()))
            .doOnNext(this::updateFloorPumpStatus)
            .then(setFloorPump());
    }

    private boolean isStale() {
        LocalDateTime lastMessage = floorPump.getUpdatedAt() == null ? LocalDateTime.MIN : floorPump.getUpdatedAt();
        return lastMessage.isBefore(LocalDateTime.now(clock).minusMinutes(5));
    }

    private void updateFloorPumpStatus(final ShellyPro4StatusResponse response) {
        boolean isOn = Boolean.TRUE.equals(response.getOutput());
        floorPump.setWorking(isOn);
        floorPump.setUpdatedAt(LocalDateTime.now(clock));

        log.info("Floor pump status was stale. Updated status: {}", isOn ? "on" : "off");
    }

    private Mono<Void> setFloorPump() {
        return Flux.fromIterable(home.rooms())
            .filter(room -> room.getName().equals(WARDROBE) || room.getName().equals(BATHROOM_UP))
            .flatMapIterable(Room::getHeaterActors)
            .filter(heaterActor -> heaterActor.getType().equals(HeaterType.FLOOR))
            .map(HeaterActor::isWorking)
            .reduce(Boolean::logicalOr)
            .flatMap(anyFloorWorks -> Flux.just(anyFloorWorks, floorPump.isWorking())
                .reduce(Boolean::logicalXor)
                .flatMap(shouldToggle -> optionallyControlPump(shouldToggle, anyFloorWorks))
            );
    }

    private Mono<Void> optionallyControlPump(final boolean shouldToggle, final boolean anyFloorWorks) {
        if (shouldToggle) {
            return shellyClient.controlFloorPump(anyFloorWorks)
                .doOnNext(response -> {
                    floorPump.setWorking(Boolean.TRUE.equals(response.getIson()));
                    floorPump.setUpdatedAt(LocalDateTime.now(clock));
                    log.info("Setting floor pump to: {}", anyFloorWorks ? "on" : "off");
                })
                .doOnError(throwable -> log.error("Error setting floor pump: {}", throwable.getMessage()))
                .then();
        }
        return Mono.empty();
    }
}
