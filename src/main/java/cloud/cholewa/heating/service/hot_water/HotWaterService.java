package cloud.cholewa.heating.service.hot_water;

import cloud.cholewa.heating.client.HotWaterSensorClient;
import cloud.cholewa.shelly.model.ShellyUniStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class HotWaterService {

    private final HotWaterSensorClient hotWaterSensorClient;

    public Mono<ResponseEntity<ShellyUniStatusResponse>> getStatus() {
        return hotWaterSensorClient.getStatus();
    }
}
