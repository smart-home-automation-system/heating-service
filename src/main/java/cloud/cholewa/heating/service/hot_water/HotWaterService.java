package cloud.cholewa.heating.service.hot_water;

import cloud.cholewa.heating.client.HotWaterSensorClient;
import cloud.cholewa.heating.db.mapper.HotWaterTemperatureMapper;
import cloud.cholewa.heating.db.model.HotWaterTemperatureEntity;
import cloud.cholewa.heating.db.repository.HotWaterTemperatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotWaterService {

    private final HotWaterSensorClient hotWaterSensorClient;
    private final HotWaterTemperatureRepository hotWaterTemperatureRepository;

    public Mono<ResponseEntity<HotWaterTemperatureEntity>> getStatus() {
        return hotWaterSensorClient.getStatus()
            .flatMap(entity -> Mono.just((Objects.requireNonNull(entity.getBody()))))
            .flatMap(response -> Mono.just(HotWaterTemperatureMapper.toEntity(response)))
            .doOnNext(entity -> log.info(entity.toString()))
            .flatMap(hotWaterTemperatureRepository::save)
            .flatMap(entity -> Mono.just(ResponseEntity.ok(entity)));
    }
}
