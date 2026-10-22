package cloud.cholewa.heating.window;

import cloud.cholewa.commons.scheduling.ScheduledPass;
import cloud.cholewa.heating.service.HomeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

//Every minute the heating of the rooms held by an open window is stopped, without waiting for a
//reading of the room (HomeService.stopHeldRooms). Scheduling is off under the test profile.
//Bounded (HAS-303): every call of a heater has the timeouts of its Shelly - a pass beyond these is
//hung and ends there; the next minute stops what is still held
@Slf4j
@Component
@RequiredArgsConstructor
public class WindowHoldScheduler {

    static final Duration LIMIT = Duration.ofSeconds(50);

    private final HomeService homeService;

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT1M")
    Mono<Void> stopHeldRooms() {
        return ScheduledPass.bounded("heating window hold", LIMIT, () -> homeService.stopHeldRooms()
            .onErrorResume(throwable -> {
                log.error("Error while stopping the heating of the rooms held by an open window", throwable);
                return Mono.empty();
            }));
    }
}
