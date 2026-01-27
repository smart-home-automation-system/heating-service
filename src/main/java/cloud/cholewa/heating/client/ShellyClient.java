package cloud.cholewa.heating.client;

import cloud.cholewa.heating.config.ShellyConfig;
import cloud.cholewa.heating.infrastructure.error.BoilerException;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.shelly.model.ShellyPro4StatusResponse;
import cloud.cholewa.shelly.model.ShellyProRelayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShellyClient {

    private final WebClient shellyWebClient;
    private final ShellyConfig shellyConfig;

    public Mono<ShellyPro4StatusResponse> getHeaterActorStatus(final HeaterType heaterType, final RoomName roomName) {
        return shellyWebClient.get()
            .uri(uriBuilder -> shellyConfig.getStatusUri(uriBuilder, heaterType, roomName))
            .retrieve()
            .bodyToMono(ShellyPro4StatusResponse.class)
            .doOnError(throwable ->
                log.error("Error while getting actor status for room: {}, heater type: {}", roomName, heaterType))
            .onErrorMap(throwable -> new BoilerException("Error while getting actor status for room: " + roomName + ", heater type: " + heaterType));
    }

    public Mono<ShellyProRelayResponse> controlHeaterActor(
        final HeaterType heaterType,
        final RoomName roomName,
        final boolean enable
    ) {
        return shellyWebClient.get()
            .uri(uriBuilder -> shellyConfig.getControlUri(uriBuilder, heaterType, roomName, enable))
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class)
            .doOnError(throwable ->
                log.error("Error while controlling actor for room: {}, heater type: {}", roomName, heaterType))
            .onErrorMap(throwable -> new BoilerException("Error while controlling actor for room: " + roomName + ", heater type: " + heaterType));
    }
}
