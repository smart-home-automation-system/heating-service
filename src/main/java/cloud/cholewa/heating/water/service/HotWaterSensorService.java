package cloud.cholewa.heating.water.service;

import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.HotWater;
import cloud.cholewa.heating.model.Pump;
import cloud.cholewa.heating.model.PumpType;
import cloud.cholewa.heating.shelly.pro.client.ShellyProClient;
import cloud.cholewa.heating.water.client.HotWaterSensorClient;
import cloud.cholewa.heating.water.db.mapper.HotWaterTemperatureMapper;
import cloud.cholewa.heating.water.db.repository.HotWaterTemperatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotWaterSensorService {

    private static final double HOT_WATER_LOW_TEMPERATURE = 38;
    private static final double HOT_WATER_HIGH_TEMPERATURE = 42;

    private final Home home;
    private final HotWaterSensorClient client;
    private final HotWaterTemperatureRepository repository;
    private final ShellyProClient shellyProClient;

    void querySensorStatus() {
        HotWater hotWater = home.getBoiler().getHotWater();
        Pump hotWaterPump = home.getBoiler().getPumps().stream()
            .filter(pump -> pump.getType().equals(PumpType.HOT_WATER_PUMP))
            .findAny().orElseThrow();

        log.info("Querying water temperatures sensor status");

        client.getStatus().flatMap(
                entity -> entity.getBody() != null
                    ? Mono.just(HotWaterTemperatureMapper.toEntity(entity.getBody()))
                    : Mono.error(() -> new IllegalArgumentException("Invalid Shelly Response"))
            )
            .flatMap(repository::save)
            .publishOn(Schedulers.boundedElastic())
            .doOnNext(entity -> {
                log.info(
                    "Updated hot water temperature is: [{}], a time: {}",
                    entity.getWaterTemperature(),
                    entity.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                );

                if (entity.getWaterTemperature() < HOT_WATER_LOW_TEMPERATURE) {
                    if (!hotWaterPump.isRunning()) {
                        hotWaterPump.setRunning(true);
                        shellyProClient.controlHotWaterPump(true).subscribe();
                    }
                }
                if (entity.getWaterTemperature() > HOT_WATER_HIGH_TEMPERATURE) {
                    if (hotWaterPump.isRunning()) {
                        hotWaterPump.setRunning(false);
                        shellyProClient.controlHotWaterPump(false).subscribe();
                    }
                }

                hotWater.setUpdateTime(entity.getTimestamp());
                hotWater.setTemperature(entity.getWaterTemperature());
            })
            .subscribe();
    }
}
