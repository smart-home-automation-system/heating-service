package cloud.cholewa.heating.shelly.actor;

import cloud.cholewa.heating.infrastructure.error.BoilerException;
import cloud.cholewa.heating.infrastructure.error.HotWaterException;
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
public class HeaterPro4Client {

    @Value("${shelly.actor.pro4.down_left.host}")
    private String SHELLY_PRO4_DOWN_LEFT;
    @Value("${shelly.actor.pro4.down_right.host}")
    private String SHELLY_PRO4_DOWN_RIGHT;
    @Value("${shelly.actor.pro4.up_left.host}")
    private String SHELLY_PRO4_UP_LEFT;
    @Value("${shelly.actor.pro4.up_right.host}")
    private String SHELLY_PRO4_UP_RIGHT;

    private final WebClient webClient;

    public Mono<ShellyProRelayResponse> controlFloorKitchen(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_DOWN_LEFT)
                .path("relay/0")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse ->
                    Mono.error(new HotWaterException("Shelly PRO4 error for [FLOOR] in kitchen"))
            )
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlLivingRoom(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_DOWN_LEFT)
                .path("relay/1")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse ->
                    Mono.error(new HotWaterException("Shelly PRO4 error for [RADIATOR] in living room"))
            )
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlFloorBathDown(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_DOWN_LEFT)
                .path("relay/2")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse ->
                    Mono.error(new HotWaterException("Shelly PRO4 error for [FLOOR] in bathroom down"))
            )
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlBathDown(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_DOWN_LEFT)
                .path("relay/3")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse ->
                    Mono.error(new HotWaterException("Shelly PRO4 error for [HEATER] in bathroom down"))
            )
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlEntrance(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_DOWN_RIGHT)
                .path("relay/0")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse ->
                    Mono.error(new HotWaterException("Shelly PRO4 error for [HEATER] in entrance"))
            )
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlCinema(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_DOWN_RIGHT)
                .path("relay/1")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse ->
                    Mono.error(new HotWaterException("Shelly PRO4 error for [HEATER] in cinema"))
            )
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlFloorGarage(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_DOWN_RIGHT)
                .path("relay/2")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse ->
                    Mono.error(new HotWaterException("Shelly PRO4 error for [HEATER] in garage"))
            )
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlPumpFloor(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_UP_LEFT)
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
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_UP_LEFT)
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

    public Mono<ShellyProRelayResponse> controlFloorBathUp(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_UP_LEFT)
                .path("relay/1")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse ->
                    Mono.error(new HotWaterException("Shelly PRO4 error for [FLOOR] in bathroom up"))
            )
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlFloorFloorWardrobe(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_UP_LEFT)
                .path("relay/2")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse ->
                    Mono.error(new HotWaterException("Shelly PRO4 error for [FLOOR] in wardrobe"))
            )
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlBathUp(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_UP_LEFT)
                .path("relay/3")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse ->
                    Mono.error(new HotWaterException("Shelly PRO4 error for [HEATER] in bathroom up"))
            )
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlBedroom(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_UP_RIGHT)
                .path("relay/0")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse ->
                    Mono.error(new HotWaterException("Shelly PRO4 error for [HEATER] in bedroom"))
            )
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlLivia(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_UP_RIGHT)
                .path("relay/1")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse ->
                    Mono.error(new HotWaterException("Shelly PRO4 error for [HEATER] in Livia's room"))
            )
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlTobi(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_UP_RIGHT)
                .path("relay/2")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse ->
                    Mono.error(new HotWaterException("Shelly PRO4 error for [HEATER] in Tobi's room"))
            )
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlOffice(final boolean enable) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_UP_RIGHT)
                .path("relay/3")
                .queryParam("turn", enable ? "on" : "off")
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse ->
                    Mono.error(new HotWaterException("Shelly PRO4 error for [HEATER] in office"))
            )
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyPro4StatusResponse> getOfficeStatus() {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_UP_RIGHT)
                .path("rpc/Switch.GetStatus")
                .queryParam("id", "3")
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, clientResponse -> Mono.error(
                    new BoilerException("Problem with communication with Shelly Pro4 [UP LEFT], detail: " + clientResponse.statusCode())
                )
            )
            .bodyToMono(ShellyPro4StatusResponse.class);
    }
}
