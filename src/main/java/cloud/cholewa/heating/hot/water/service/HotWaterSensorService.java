package cloud.cholewa.heating.hot.water.service;

import cloud.cholewa.heating.model.HotWater;
import cloud.cholewa.heating.model.Pump;
import cloud.cholewa.heating.shelly.sensor.HotWaterSensorClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotWaterSensorService {

    private static final double CIRCULATION_TEMPERATURE_MAX = 22;
    private final HotWater hotWater;

    private final HotWaterSensorClient hotWaterSensorClient;

    public void querySensorStatus() {
        Pump circulationPump = hotWater.circulation().pump();

        log.info("Querying water temperatures sensor status");

        hotWaterSensorClient.getStatus()
            .flatMap(response -> {
                hotWater.temperature().setUpdatedAt(LocalDateTime.now());
                hotWater.temperature().setValue(Objects.requireNonNull(
                    Objects.requireNonNull(response.getExtTemperature()).get("0").gettC()));

                hotWater.circulation().temperature().setUpdatedAt(LocalDateTime.now());
                hotWater.circulation().temperature().setValue(Objects.requireNonNull(
                    Objects.requireNonNull(response.getExtTemperature()).get("1").gettC()));

                circulationPump.setRunning(Boolean.TRUE.equals(Objects.requireNonNull(
                    response.getRelays()).get(1).getIson()));

                return Mono.just(response);
            })
            .doOnNext(response ->
                log.info(
                    "Received updated water temperature: [{} C] and circulation temperature: [{} C]",
                    hotWater.temperature().getValue(),
                    hotWater.circulation().temperature().getValue()
                )
            )
            .flatMap(response -> {
                optionallyTurnOnCirculationPump();
                return Mono.just(response);
            })
            .subscribe();
    }

    private void optionallyTurnOnCirculationPump() {
        if (LocalTime.now().isAfter(LocalTime.of(5, 0))
            && LocalTime.now().isBefore(LocalTime.of(23, 59))
        ) {
            if (hotWater.circulation().temperature().getValue() < CIRCULATION_TEMPERATURE_MAX) {
                if (!hotWater.circulation().pump().isRunning()) {
                    hotWaterSensorClient.enableCirculationPump().subscribe();
                }
            }
        }
    }
}
