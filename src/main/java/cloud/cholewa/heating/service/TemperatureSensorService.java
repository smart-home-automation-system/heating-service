package cloud.cholewa.heating.service;

import cloud.cholewa.heating.config.SensorMonitorProperties;
import cloud.cholewa.heating.db.repository.TemperatureRepository;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.TemperatureSensorReply;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.LocalDateTime;

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
        return Flux.defer(() -> {
            final LocalDateTime staleBefore = LocalDateTime.now(clock).minus(properties.staleAfter());

            return Flux.fromIterable(home.rooms())
                .map(Room::getName)
                //one room at a time, so the whole pass holds a single connection of the pool
                .concatMap(room -> temperatureRepository.findFirstByRoomOrderByDateDesc(room.getValue())
                    .map(entity -> new TemperatureSensorReply(
                        room,
                        entity.date(),
                        entity.date().isBefore(staleBefore),
                        properties.mutedRooms().contains(room)
                    )));
        });
    }
}
