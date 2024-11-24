package cloud.cholewa.heating.shelly.sensor;

import cloud.cholewa.heating.infrastructure.error.HotWaterException;
import cloud.cholewa.shelly.model.ShellyUniStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class HotWaterSensorClient {

    @Value("${shelly.sensor.uni.hot-water.host}")
    private String hotWaterSensorHost;

    private final WebClient hotWaterSensorClient;

    public Mono<ShellyUniStatusResponse> getStatus() {
        log.info("Getting status from hot water sensor");
        return hotWaterSensorClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .scheme("http")
                .host(hotWaterSensorHost)
                .path("status")
                .build()
            )
            .retrieve()
            .onStatus(
                HttpStatusCode::isError,
                clientResponse -> Mono.error(
                    new HotWaterException("Problem with communication with hot water sensor, detail: " + clientResponse.statusCode())
                )
            )
            .bodyToMono(ShellyUniStatusResponse.class);
    }

    public Mono<Void> enableCirculationPump() {
        log.info("Enabling circulation pump");

        return hotWaterSensorClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(hotWaterSensorHost)
                .path("relay/1")
                .queryParam("turn", "on")
                .build()
            )
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse -> Mono.error(
                    new HotWaterException(
                        "Problem with communication with hot water sensor (can't turn on circulation pump," +
                            " detail: " + clientResponse.statusCode())
                )
            )
            .bodyToMono(Void.class);
    }
}
