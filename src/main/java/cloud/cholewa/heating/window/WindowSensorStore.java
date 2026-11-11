package cloud.cholewa.heating.window;

import cloud.cholewa.heating.db.model.WindowSensorReadingEntity;
import cloud.cholewa.heating.db.repository.WindowSensorReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

//The last reading of every window sensor in window_sensor_reading (HAS-419), so that a restart keeps what the service
//knew. Nothing of it may stop what calls it: a write, a read or a delete that fails is a WARN and completes - the
//readings in memory hold all the same, and the next change of a sensor writes it again. One statement per call: the
//pool of 2 is shared with the listener
@Slf4j
@Component
@RequiredArgsConstructor
public class WindowSensorStore {

    //a write is part of the chain of the device listener (HAS-421): one hung on a dead connection must not hold its
    //message for good - the bound of the same write in blinds-service (HAS-420)
    static final Duration WRITE_TIMEOUT = Duration.ofSeconds(30);

    private final WindowSensorReadingRepository repository;
    private final Clock clock;

    /** A reading read back from the table, by the registry id of its sensor. */
    record Stored(long sensorId, WindowSensors.Reading reading) {
    }

    //a reading that says open or closed; deferred, so nothing runs before it is subscribed to
    Mono<Void> save(final long sensorId, final WindowSensors.Reading reading) {
        return Mono.defer(() -> repository.upsert(sensorId, reading.open(), reading.updatedAt(), reading.openSince(),
                LocalDateTime.now(clock)))
            .then()
            .timeout(WRITE_TIMEOUT)
            .onErrorResume(e -> {
                log.warn("The reading of window sensor {} could not be stored, a restart forgets it: {}", sensorId, e.toString());
                return Mono.empty();
            });
    }

    //every stored reading as not confirmed: no gateway has stood for it since the start
    Flux<Stored> load() {
        return Flux.defer(repository::findAll)
            .map(WindowSensorStore::toStored)
            .onErrorResume(e -> {
                log.warn("The stored window sensor readings could not be read, the start knows none: {}", e.toString());
                return Flux.empty();
            });
    }

    //the rows of the sensors the registry lists, and no others; never called with none (WindowSensors keeps them then)
    Mono<Void> retain(final Set<Long> sensorIds) {
        return Mono.defer(() -> repository.deleteOthers(sensorIds))
            .then()
            .onErrorResume(e -> {
                log.warn("The readings of the window sensors gone from the registry could not be deleted: {}", e.toString());
                return Mono.empty();
            });
    }

    private static Stored toStored(final WindowSensorReadingEntity row) {
        return new Stored(row.sensorId(),
            new WindowSensors.Reading(row.open(), row.updatedAt(), null, Boolean.TRUE.equals(row.open()) ? row.openSince() : null));
    }
}
