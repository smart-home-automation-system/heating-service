package cloud.cholewa.heating.service.hot_water;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@RequiredArgsConstructor
public class HotWaterSensorScheduler {

    private final HotWaterSensorService hotWaterSensorService;

    @Scheduled(fixedRateString = "${jobs.hot-water.poolingInterval}")
    void schedule() {
        hotWaterSensorService.querySensorStatus();
    }
}
