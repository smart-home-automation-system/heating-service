package cloud.cholewa.heating.window;

import cloud.cholewa.commons.scheduling.ScheduledPass;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

//The window sensors and their gateways read at the start and every five minutes, and after each
//read the silent sensors checked. Spring subscribes again to the same Mono for every run, which is
//why both are deferred inside. Scheduling is off under the test profile (SchedulingConfig).
//Bounded (HAS-303): the registry and every gateway have their timeouts, every notification is cut
//off after 30 s - a pass beyond four minutes is hung and ends there; the next one reads again
@Slf4j
@Component
@RequiredArgsConstructor
public class WindowSensorsRefresh {

    static final Duration LIMIT = Duration.ofMinutes(4);

    private final WindowSensors windowSensors;
    private final OpeningSensorMonitor openingSensorMonitor;

    @Scheduled(initialDelayString = "PT0S", fixedDelayString = "PT5M")
    Mono<Void> refresh() {
        return ScheduledPass.bounded("heating window sensors", LIMIT, () -> windowSensors.refresh()
            .then(openingSensorMonitor.check())
            .onErrorResume(throwable -> {
                log.error("Error while checking the window sensors", throwable);
                return Mono.empty();
            }));
    }
}
