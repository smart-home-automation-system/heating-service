package cloud.cholewa.heating.hot.water.service;

import cloud.cholewa.heating.model.HotWater;
import cloud.cholewa.heating.model.WaterCirculation;
import cloud.cholewa.heating.pump.service.FurnaceService;
import cloud.cholewa.heating.pump.service.HotWaterPumpService;
import cloud.cholewa.shelly.model.ShellyPro4StatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotWaterService {

    private final HotWater hotWater;
    private final HotWaterSensorService hotWaterSensorService;
    private final HotWaterPumpService hotWaterPumpService;
    private final FurnaceService furnaceService;

    public Mono<HotWater> getHotWaterStatus() {
        return Mono.just(hotWater);
    }

    public Mono<WaterCirculation> getWaterCirculationStatus() {
        return Mono.just(hotWater.circulation());
    }

    public Mono<ShellyPro4StatusResponse> executeHotWaterUpdate() {
        return hotWaterSensorService.handleSensor()
            .flatMap(response -> hotWaterPumpService.handleHotWaterPump())
            .delayElement(Duration.ofSeconds(1))
            .flatMap(response -> furnaceService.handleFurnace());
    }
}
