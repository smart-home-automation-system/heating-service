package cloud.cholewa.heating.air.client;

import cloud.cholewa.shelly.model.ShellyProRelayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class AirShellyProClient {

    private static final String SHELLY_PRO4_DOWN_LEFT = "10.78.30.39";
    private static final String SHELLY_PRO4_DOWN_RIGHT = "10.78.30.19";
    private static final String SHELLY_PRO4_UP_LEFT = "10.78.30.89";
    private static final String SHELLY_PRO4_UP_RIGHT = "10.78.30.26";

    private final WebClient webClient;

    public Mono<ShellyProRelayResponse> controlFloorKitchen(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_DOWN_LEFT)
                .path("relay/0")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlLivingRoom(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_DOWN_LEFT)
                .path("relay/1")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlFloorBathDown(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_DOWN_LEFT)
                .path("relay/2")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlBathDown(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_DOWN_LEFT)
                .path("relay/3")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlEntrance(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_DOWN_RIGHT)
                .path("relay/0")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlCinema(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_DOWN_RIGHT)
                .path("relay/1")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlFloorGarage(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_DOWN_RIGHT)
                .path("relay/2")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlPumpFloor(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_UP_LEFT)
                .path("relay/0")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlFloorBathUp(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_UP_LEFT)
                .path("relay/1")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlFloorFloorWardrobe(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_UP_LEFT)
                .path("relay/2")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlBathUp(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_UP_LEFT)
                .path("relay/3")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlBedroom(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_UP_RIGHT)
                .path("relay/0")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlLivia(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_UP_RIGHT)
                .path("relay/1")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlTobi(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_UP_RIGHT)
                .path("relay/2")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlOffice(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_UP_RIGHT)
                .path("relay/3")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class);
    }
}
