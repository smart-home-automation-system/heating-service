package cloud.cholewa.heating.hot.water.service;

import cloud.cholewa.heating.hot.water.client.HotWaterSensorClient;
import cloud.cholewa.heating.model.HotWater;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotWaterSensorService {

    private static final double HOT_WATER_LOW_TEMPERATURE = 38;
    private static final double HOT_WATER_HIGH_TEMPERATURE = 42;
    private static final double CIRCULATION_TEMPERATURE_MAX = 25;

    private final HotWaterSensorClient hotWaterSensorClient;

    private final HotWater hotWater;

    public void querySensorStatus() {
        log.info("Querying water temperatures sensor status");

        hotWaterSensorClient.getStatus()
            .flatMap(response -> {
                hotWater.temperature().setUpdatedAt(LocalDateTime.now());
                hotWater.temperature().setValue(Objects.requireNonNull(
                    Objects.requireNonNull(response.getExtTemperature()).get("0").gettC()));

                hotWater.circulation().temperature().setUpdatedAt(LocalDateTime.now());
                hotWater.circulation().temperature().setValue(Objects.requireNonNull(
                    Objects.requireNonNull(response.getExtTemperature()).get("1").gettC()));

                return Mono.just(response);
            })
            .doOnNext(response ->
                log.info(
                    "Received updated water temperature: [{} C] and circulation temperature: [{} C]",
                    hotWater.temperature().getValue(),
                    hotWater.circulation().temperature().getValue()
                )
            )
            .subscribe();
//
//        client.getStatus().flatMap(
//                entity -> entity.getBody() != null
//                    ? Mono.just(HotWaterTemperatureMapper.toEntity(entity.getBody()))
//                    : Mono.error(() -> new IllegalArgumentException("Invalid Shelly Response"))
//            )
//            .flatMap(repository::save)
//            .publishOn(Schedulers.boundedElastic())
//            .doOnNext(entity -> {
//                log.info(
//                    "Updated hot water temperature is: [{}], a time: {}",
//                    entity.getWaterTemperature(),
//                    entity.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
//                );
//
//                if (entity.getWaterTemperature() < HOT_WATER_LOW_TEMPERATURE) {
//                    if (!hotWaterPump.isRunning()) {
//                        hotWaterPump.setRunning(true);
//                        shellyProClient.controlHotWaterPump(true).subscribe();
//                    }
//                }
//                if (entity.getWaterTemperature() > HOT_WATER_HIGH_TEMPERATURE) {
//                    if (hotWaterPump.isRunning()) {
//                        hotWaterPump.setRunning(false);
//                        shellyProClient.controlHotWaterPump(false).subscribe();
//                    }
//                }
//
//                hotWater.setUpdateTime(entity.getTimestamp());
//                hotWater.setTemperature(entity.getWaterTemperature());
//            })
//            .subscribe();
    }
}
