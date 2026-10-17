package cloud.cholewa.heating.window;

import cloud.cholewa.heating.service.HomeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

//Every minute the heating of the rooms held by an open window is stopped, without waiting for a
//reading of the room (HomeService.stopHeldRooms). Scheduling is off under the test profile
@Slf4j
@Component
@RequiredArgsConstructor
public class WindowHoldScheduler {

    private final HomeService homeService;

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT1M")
    Mono<Void> stopHeldRooms() {
        return homeService.stopHeldRooms()
            .onErrorResume(throwable -> {
                log.error("Error while stopping the heating of the rooms held by an open window", throwable);
                return Mono.empty();
            });
    }
}
