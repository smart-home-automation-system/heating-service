package cloud.cholewa.heating.hot.water.service;

import cloud.cholewa.heating.model.HotWater;
import cloud.cholewa.heating.model.Pump;
import cloud.cholewa.heating.shelly.sensor.HotWaterSensorClient;
import cloud.cholewa.shelly.model.ShellyUniStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

import static cloud.cholewa.heating.model.HeatingTemperatures.CIRCULATION_TEMPERATURE_MAX;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotWaterSensorService {

    private final HotWater hotWater;
    private final HotWaterSensorClient hotWaterSensorClient;

    public Mono<Void> handle() {
        return hotWaterSensorClient.getStatus()
            .flatMap(this::updateHotWaterStatus)
            .doOnNext(this::logHotWaterUpdateStatus)
            .flatMap(response -> {
                optionallyTurnOnCirculationPump();
                return Mono.empty();
            });
    }

    private Mono<ShellyUniStatusResponse> updateHotWaterStatus(final ShellyUniStatusResponse response) {
        Pump circulationPump = hotWater.circulation().pump();

        hotWater.temperature().setUpdatedAt(LocalDateTime.now());
        hotWater.temperature().setValue(Objects.requireNonNull(
            Objects.requireNonNull(response.getExtTemperature()).get("0").gettC()));

        hotWater.circulation().temperature().setUpdatedAt(LocalDateTime.now());
        hotWater.circulation().temperature().setValue(Objects.requireNonNull(
            Objects.requireNonNull(response.getExtTemperature()).get("1").gettC()));

        circulationPump.setRunning(Boolean.TRUE.equals(Objects.requireNonNull(
            response.getRelays()).get(1).getIson()));

        return Mono.just(response);
    }

    private void logHotWaterUpdateStatus(final ShellyUniStatusResponse response) {
        log.info(
            "Status update [HOT WATER] temperature: [{}] and [CIRCULATION] temperature: [{}]",
            hotWater.temperature().getValue(),
            hotWater.circulation().temperature().getValue()
        );
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
