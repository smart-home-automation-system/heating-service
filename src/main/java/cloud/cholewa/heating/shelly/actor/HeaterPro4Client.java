package cloud.cholewa.heating.shelly.actor;

import cloud.cholewa.heating.infrastructure.error.BoilerException;
import cloud.cholewa.heating.infrastructure.error.HotWaterException;
import cloud.cholewa.shelly.model.ShellyPro4StatusResponse;
import cloud.cholewa.shelly.model.ShellyProRelayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class HeaterPro4Client {

    private final WebClient webClient;
    private final HeaterPro4Config heaterPro4Config;

    public Mono<ShellyProRelayResponse> setHeaterActor(
        final HeaterPro4Config.HeaterPro4Data heaterData,
        final boolean enable
    ) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(heaterData.host())
                .path("relay/" + heaterData.relay())
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse ->
                    Mono.error(new BoilerException("Shelly PRO4 communication error on IP: " + heaterData.host()))
            )
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyPro4StatusResponse> getHeaterActorStatus(final HeaterPro4Config.HeaterPro4Data heaterData) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(heaterData.host())
                .path("rpc/Switch.GetStatus")
                .queryParam("id", heaterData.relay())
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse ->
                    Mono.error((new BoilerException("Shelly PRO4 communication error on IP: " + heaterData.host()))
                    )
            )
            .bodyToMono(ShellyPro4StatusResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlPumpFloor(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(heaterPro4Config.getSHELLY_PRO4_UP_LEFT())
                .path("relay/0")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse ->
                    Mono.error(new HotWaterException("Shelly PRO4 error for [PUMP] for floor"))
            )
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyPro4StatusResponse> getFloorPumpStatus() {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(heaterPro4Config.getSHELLY_PRO4_UP_LEFT())
                .path("rpc/Switch.GetStatus")
                .queryParam("id", "0")
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse -> Mono.error(
                    new BoilerException("Problem with communication with Shelly Pro4 in up left, detail: " + clientResponse.statusCode())
                )
            )
            .bodyToMono(ShellyPro4StatusResponse.class);
    }
}
