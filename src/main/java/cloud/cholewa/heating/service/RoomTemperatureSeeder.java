package cloud.cholewa.heating.service;

import cloud.cholewa.heating.db.model.TemperatureEntity;
import cloud.cholewa.heating.db.repository.TemperatureRepository;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.Temperature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gives every room the last temperature stored for it, with the time it was measured at, so that
 * the rooms are not blank after a start of the service until each sensor reports again.
 * <p>
 * Only what a reader sees changes. Nothing here starts a control pass, and a pass never acts on
 * a seeded value: it begins by writing the reading that triggered it. The heaters stay as they
 * are - what a relay does is known only from the relay.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomTemperatureSeeder {

    private final Home home;
    private final TemperatureRepository temperatureRepository;

    public Mono<Void> seed() {
        return Flux.defer(() -> Flux.fromIterable(home.rooms()))
            //one room at a time, so the whole pass holds a single connection of the pool
            .concatMap(this::seedRoom)
            .then();
    }

    private Mono<Void> seedRoom(final Room room) {
        return temperatureRepository.findFirstByRoomOrderByDateDesc(room.getName().getValue())
            .doOnNext(entity -> seedRoom(room, entity))
            //a room that cannot be read stays blank until its sensor reports; the others are still seeded
            .onErrorResume(throwable -> {
                log.error("Error while reading the last temperature of room: {}", room.getName(), throwable);
                return Mono.empty();
            })
            .then();
    }

    private void seedRoom(final Room room, final TemperatureEntity entity) {
        final Temperature temperature = room.getTemperature();

        if (temperature == null || entity.temperature() == null || entity.date() == null) {
            return;
        }
        //the listener is consuming by now: a reading that arrived in the meantime is newer than the
        //row read here and must stay
        if (temperature.getUpdatedAt() != null) {
            return;
        }
        temperature.setValue(entity.temperature());
        temperature.setUpdatedAt(entity.date());
        log.info(
            "Room: {} starts with its last stored temperature: {}°C of {}",
            room.getName(), entity.temperature(), entity.date()
        );
    }
}
