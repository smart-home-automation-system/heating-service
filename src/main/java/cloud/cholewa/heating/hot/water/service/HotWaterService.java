package cloud.cholewa.heating.hot.water.service;

import cloud.cholewa.heating.model.HotWater;
import cloud.cholewa.heating.model.WaterCirculation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotWaterService {

    private final HotWater hotWater;

    public Mono<HotWater> getHotWaterStatus() {
        return Mono.just(hotWater);
    }

    public Mono<WaterCirculation> getWaterCirculationStatus() {
        return Mono.just(hotWater.circulation());
    }
}
