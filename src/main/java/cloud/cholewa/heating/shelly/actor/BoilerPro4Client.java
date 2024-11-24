package cloud.cholewa.heating.shelly.actor;

import cloud.cholewa.shelly.model.ShellyProRelayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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



    public Mono<ShellyProRelayResponse> controlFurnace(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_BOILER_HOST)
                .path("relay/0")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class);
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

    public Mono<ShellyProRelayResponse> controlHeatingPump(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_BOILER_HOST)
                .path("relay/2")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class);
    }
}

//http://10.78.30.20/rpc/Switch.GetStatus?id=0