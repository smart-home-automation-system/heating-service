package cloud.cholewa.heating.service;

import cloud.cholewa.heating.config.SensorMonitorProperties;
import cloud.cholewa.heating.db.repository.TemperatureRepository;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.TemperatureSensorReply;
import cloud.cholewa.home.model.RoomName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemperatureSensorService {

    private final Clock clock;
    private final Home home;
    private final SensorMonitorProperties properties;
    private final TemperatureRepository temperatureRepository;

    /**
     * The last reading of every room that has ever reported. A room without any stored
     * temperature has no sensor as far as this service can tell, so it is left out.
     */
    public Flux<TemperatureSensorReply> querySensors() {
        return querySensors(false);
    }

    /**
     * The same, for a caller that would rather see the rooms it can than none at all: a room
     * whose reading cannot be read is logged and left out instead of ending the whole pass.
     */
    public Flux<TemperatureSensorReply> queryReadableSensors() {
        return querySensors(true);
    }

    private Flux<TemperatureSensorReply> querySensors(final boolean skipUnreadable) {
        return Flux.defer(() -> {
            final LocalDateTime staleBefore = LocalDateTime.now(clock).minus(properties.staleAfter());

            return Flux.fromIterable(home.rooms())
                .map(Room::getName)
                //one room at a time, so the whole pass holds a single connection of the pool
                .concatMap(room -> {
                    final Mono<TemperatureSensorReply> sensor = querySensor(room, staleBefore);

                    return skipUnreadable
                        ? sensor.onErrorResume(throwable -> {
                            log.error("Error while reading the last temperature of room: {}", room, throwable);
                            return Mono.empty();
                        })
                        : sensor;
                });
        });
    }

    private Mono<TemperatureSensorReply> querySensor(final RoomName room, final LocalDateTime staleBefore) {
        return temperatureRepository.findFirstByRoomOrderByDateDesc(room.getValue())
            .map(entity -> new TemperatureSensorReply(
                room,
                entity.date(),
                entity.date().isBefore(staleBefore),
                properties.mutedRooms().contains(room)
            ));
    }
}
