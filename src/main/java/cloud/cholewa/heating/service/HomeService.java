package cloud.cholewa.heating.service;

import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.home.model.RoomName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeService {

    private final Home home;
    private final ScheduleService scheduleService;
    private final HeatingService heatingService;

    /**
     * 1. 
     *
     */
    public Mono<Void> processRoomTemperature(final RoomName roomName, final double temperature) {
        return Flux.fromIterable(home.rooms())
            .filter(room -> room.getName().equals(roomName))
            .doOnNext(room -> updateRoomTemperature(room, temperature))
            .flatMap(scheduleService::processSchedule)
            .flatMap(heatingService::processHeatingRequest)
            .then();
    }

    private void updateRoomTemperature(final Room room, final double temperature) {
        room.getTemperature().setUpdatedAt(LocalDateTime.now());
        room.getTemperature().setValue(temperature);
    }
}
