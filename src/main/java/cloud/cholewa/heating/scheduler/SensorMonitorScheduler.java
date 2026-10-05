package cloud.cholewa.heating.scheduler;

import cloud.cholewa.heating.service.SensorMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class SensorMonitorScheduler {

    private final SensorMonitorService sensorMonitorService;

    @Scheduled(cron = "${heating.sensor-monitor.cron}")
    Mono<Void> checkSensors() {
        return sensorMonitorService.checkSensors()
            .doOnSubscribe(subscription -> log.info("Checking the temperature sensors for missing readings"))
            .onErrorResume(throwable -> {
                log.error("Error while checking the temperature sensors", throwable);
                return Mono.empty();
            });
    }
}
