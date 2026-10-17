package cloud.cholewa.heating.window;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

//The window sensors and their gateways read at the start and every five minutes, and after each
//read the silent sensors checked. Spring subscribes again to the same Mono for every run, which is
//why both are deferred inside. Scheduling is off under the test profile (SchedulingConfig)
@Slf4j
@Component
@RequiredArgsConstructor
public class WindowSensorsRefresh {

    private final WindowSensors windowSensors;
    private final OpeningSensorMonitor openingSensorMonitor;

    @Scheduled(initialDelayString = "PT0S", fixedDelayString = "PT5M")
    Mono<Void> refresh() {
        return windowSensors.refresh()
            .then(openingSensorMonitor.check())
            .onErrorResume(throwable -> {
                log.error("Error while checking the window sensors", throwable);
                return Mono.empty();
            });
    }
}
