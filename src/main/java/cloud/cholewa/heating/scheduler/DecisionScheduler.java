package cloud.cholewa.heating.scheduler;

import cloud.cholewa.commons.scheduling.ScheduledPass;
import cloud.cholewa.heating.service.HomeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

//Every minute every room is decided without waiting for its reading (HAS-352, HomeService.decideRooms): a
//boundary of a schedule, the switch of the house and an open window act within the minute. It replaces the
//pass of the held rooms of HAS-232 (WindowHoldScheduler), which the decision of every room includes - a held
//room is out of schedule there. Scheduling is off under the test profile.
//Bounded (HAS-303): every call of a heater has the timeouts of its Shelly - a pass beyond these is hung and
//ends there; the next minute decides what is still undecided
@Slf4j
@Component
@RequiredArgsConstructor
public class DecisionScheduler {

    static final Duration LIMIT = Duration.ofSeconds(50);

    private final HomeService homeService;

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT1M")
    Mono<Void> decideRooms() {
        return ScheduledPass.bounded("heating decisions", LIMIT, () -> homeService.decideRooms()
            .onErrorResume(throwable -> {
                log.error("Error while deciding the heating of the rooms", throwable);
                return Mono.empty();
            }));
    }
}
