package cloud.cholewa.heating.client;

import cloud.cholewa.heating.infrastructure.error.BoilerException;
import cloud.cholewa.heating.infrastructure.error.HotWaterException;
import cloud.cholewa.heating.config.ShellyConfig;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.home.model.RoomName;
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
public class ShellyClient {

    private final WebClient shellyWebClient;
    private final ShellyConfig shellyConfig;

    public Mono<ShellyPro4StatusResponse> getHeaterActorStatus(
        final HeaterType heaterType,
        final RoomName roomName
    ) {
        return Mono.empty();
    }

    public Mono<ShellyProRelayResponse> controlHeaterActor(
        final HeaterType heaterType,
        final RoomName roomName,
        final boolean enable
    ) {
        return Mono.empty();
    }


    public Mono<ShellyProRelayResponse> controlHeaterActor(
        final ShellyConfig.HeaterPro4Data heaterData,
        final boolean enable
    ) {
        return shellyWebClient.get()
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

    public Mono<ShellyPro4StatusResponse> getHeaterActorStatus(final ShellyConfig.HeaterPro4Data heaterData) {
        return shellyWebClient.get()
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
        return shellyWebClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(shellyConfig.getSHELLY_PRO4_UP_LEFT())
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
        return shellyWebClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(shellyConfig.getSHELLY_PRO4_UP_LEFT())
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
