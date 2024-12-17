package cloud.cholewa.heating.hot.water.cron;

import cloud.cholewa.heating.hot.water.service.HotWaterSensorService;
import cloud.cholewa.heating.pump.service.FurnaceService;
import cloud.cholewa.heating.pump.service.HotWaterPumpService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@RequiredArgsConstructor
public class HotWaterSensorScheduler {

    private final HotWaterSensorService hotWaterSensorService;
    private final HotWaterPumpService hotWaterPumpService;
    private final FurnaceService furnaceService;

    @Scheduled(fixedRateString = "${jobs.hot-water.poolingInterval}", initialDelayString = "PT10s")
    void schedule() {
        hotWaterSensorService.handleSensor()
            .then(hotWaterPumpService.handleHotWaterPump())
            .then(furnaceService.handleFurnace())
            .subscribe();
    }
}
