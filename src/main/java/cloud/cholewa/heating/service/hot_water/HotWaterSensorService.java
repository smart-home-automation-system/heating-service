package cloud.cholewa.heating.service.hot_water;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotWaterSensorService {

    void querySensorStatus() {
        log.info("querying water temperatures sensor status");
    }
}
