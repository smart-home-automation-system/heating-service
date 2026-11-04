package cloud.cholewa.heating.window;

import cloud.cholewa.commons.scheduling.ScheduledPass;
import cloud.cholewa.heating.thermometer.ThermometerReadings;
import cloud.cholewa.heating.thermometer.Thermometers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

//The window sensors and their gateways read at the start and every five minutes, and after each
//read the silent sensors checked, then those that may be stuck open (HAS-350). Spring subscribes again
//to the same Mono for every run, which is why each check is deferred inside. Scheduling is off under the test profile (SchedulingConfig).
//Bounded (HAS-303): the registry and every gateway have their timeouts, every notification is cut
//off after 30 s - a pass beyond four minutes is hung and ends there; the next one reads again
@Slf4j
@Component
@RequiredArgsConstructor
public class WindowSensorsRefresh {

    static final Duration LIMIT = Duration.ofMinutes(4);

    private final WindowSensors windowSensors;
    private final OpeningSensorMonitor openingSensorMonitor;
    private final StuckOpenMonitor stuckOpenMonitor;
    private final Thermometers thermometers;
    private final ThermometerReadings thermometerReadings;

    @Scheduled(initialDelayString = "PT0S", fixedDelayString = "PT5M")
    Mono<Void> refresh() {
        //the Shelly thermometers of the registry first (HAS-360): one call to database-service, so a gateway that hangs
        //the window sensors to the bound never keeps them stale; a failed read keeps the last ones
        return ScheduledPass.bounded("heating window sensors", LIMIT, () -> thermometers.refresh()
            .then(windowSensors.refresh())
            //the values of the BLU thermometers the gateways hold (HAS-364): the first reading of a room after a start
            .then(Mono.defer(() -> thermometerReadings.fromGateways(windowSensors.gatewayComponents())))
            .then(openingSensorMonitor.check())
            //the sensors that may be stuck open (HAS-350), after the silent ones: it never fails the pass
            .then(stuckOpenMonitor.check())
            .onErrorResume(throwable -> {
                log.error("Error while checking the window sensors", throwable);
                return Mono.empty();
            }));
    }
}
