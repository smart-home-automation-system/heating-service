package cloud.cholewa.heating.service.hot_water;

import cloud.cholewa.heating.client.HotWaterSensorClient;
import cloud.cholewa.heating.db.mapper.HotWaterTemperatureMapper;
import cloud.cholewa.heating.db.repository.HotWaterTemperatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotWaterSensorService {

    private final HotWaterSensorClient client;
    private final HotWaterTemperatureRepository repository;

    void querySensorStatus() {
        log.info("querying water temperatures sensor status");

        client.getStatus().flatMap(
                entity -> entity.getBody() != null
                    ? Mono.just(HotWaterTemperatureMapper.toEntity(entity.getBody()))
                    : Mono.error(() -> new IllegalArgumentException("Invalid Shelly Response"))
            )
            .flatMap(repository::save)
            .subscribe();
    }
}
