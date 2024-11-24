package cloud.cholewa.heating.shelly.actor;

import cloud.cholewa.heating.infrastructure.error.BoilerException;
import cloud.cholewa.shelly.model.ShellyPro4StatusResponse;
import cloud.cholewa.shelly.model.ShellyProRelayResponse;
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
public class BoilerPro4Client {

    @Value("${shelly.actor.pro4.boiler.host}")
    private String SHELLY_PRO4_BOILER_HOST;

    private final WebClient webClient;

    public Mono<ShellyProRelayResponse> controlFireplacePump(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_BOILER_HOST)
                .path("relay/3")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyPro4StatusResponse> getFireplacePumpStatus() {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_BOILER_HOST)
                .path("rpc/Switch.GetStatus")
                .queryParam("id", "3")
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse -> Mono.error(
                    new BoilerException("Problem with communication with Shelly Pro4 in boiler, detail: " + clientResponse.statusCode())
                )
            )
            .bodyToMono(ShellyPro4StatusResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlFurnace(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_BOILER_HOST)
                .path("relay/0")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyPro4StatusResponse> getFurnaceStatus() {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_BOILER_HOST)
                .path("rpc/Switch.GetStatus")
                .queryParam("id", "0")
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse -> Mono.error(
                    new BoilerException("Problem with communication with Shelly Pro4 in boiler, detail: " + clientResponse.statusCode())
                )
            )
            .bodyToMono(ShellyPro4StatusResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlHotWaterPump(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_BOILER_HOST)
                .path("relay/1")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyPro4StatusResponse> getHotWaterPumpStatus() {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_BOILER_HOST)
                .path("rpc/Switch.GetStatus")
                .queryParam("id", "1")
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse -> Mono.error(
                    new BoilerException("Problem with communication with Shelly Pro4 in boiler, detail: " + clientResponse.statusCode())
                )
            )
            .bodyToMono(ShellyPro4StatusResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlHeatingPump(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_BOILER_HOST)
                .path("relay/2")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyPro4StatusResponse> getHeatingPumpStatus() {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_BOILER_HOST)
                .path("rpc/Switch.GetStatus")
                .queryParam("id", "2")
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse -> Mono.error(
                    new BoilerException("Problem with communication with Shelly Pro4 in boiler, detail: " + clientResponse.statusCode())
                )
            )
            .bodyToMono(ShellyPro4StatusResponse.class);
    }
}
