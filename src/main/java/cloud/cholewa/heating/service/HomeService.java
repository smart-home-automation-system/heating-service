package cloud.cholewa.heating.service;

import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Room;
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
public class HomeService {

    private final Clock clock;
    private final Home home;
    private final ScheduleService scheduleService;
    private final HeatingService heatingService;
    
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
                log.error("Error processing heating request for room: {}", roomName
                );
                return Mono.empty();
            })
            .then();
    }

    private void updateRoomTemperature(final Room room, final double temperature) {
        room.getTemperature().setUpdatedAt(LocalDateTime.now(clock));
        room.getTemperature().setValue(temperature);
    }
}
