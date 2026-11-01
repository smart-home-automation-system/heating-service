package cloud.cholewa.heating.client;

import cloud.cholewa.heating.config.RelayConfig;
import cloud.cholewa.heating.config.ShellyConfig;
import cloud.cholewa.heating.infrastructure.error.BoilerException;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.shelly.http.ShellyGen1Client;
import cloud.cholewa.shelly.model.Relay;
import cloud.cholewa.shelly.model.ShellyPro4StatusResponse;
import cloud.cholewa.shelly.model.ShellyProRelayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

//The relays of the heating. A heater is a relay of one of the four Pro 4 (Switch.GetStatus, /relay/<n>) - or, since
//HAS-361, a standalone radiator: a Shelly Gen1 plug of its own (RelayConfig.standalone, the Plug S of sanctum), read and
//switched through ShellyGen1Client (/relay/0) with the timeouts of the Pro 4 calls (AppConfig). Its answer is told in the
//models of a Pro 4 relay, so the control loop does not tell the two apart. Every failure is a BoilerException
@Slf4j
@Component
@RequiredArgsConstructor
public class ShellyClient {

    private static final int STANDALONE_CHANNEL = 0;

    private final WebClient shellyWebClient;
    private final ShellyConfig shellyConfig;
    private final RelayConfig relayConfig;
    private final ShellyGen1Client standaloneHeaterClient;

    public Mono<ShellyPro4StatusResponse> getHeaterActorStatus(final HeaterType heaterType, final RoomName roomName) {
        return relayConfig.standaloneAddress(roomName, heaterType)
            .<Mono<ShellyPro4StatusResponse>>map(address -> standaloneHeaterClient.relayStatus(address, STANDALONE_CHANNEL)
                .map(relay -> ShellyPro4StatusResponse.builder().output(ison(relay)).build()))
            .orElseGet(() -> pro4Status(heaterType, roomName))
            .doOnError(throwable ->
                log.error(
                    "Error while getting actor status for room: {}, heater type: {}: {}",
                    roomName, heaterType, throwable.toString()
                ))
            .onErrorMap(throwable -> new BoilerException(
                "Error while getting actor status for room: " + roomName + ", heater type: " + heaterType, throwable));
    }

    private Mono<ShellyPro4StatusResponse> pro4Status(final HeaterType heaterType, final RoomName roomName) {
        return shellyWebClient.get()
            .uri(uriBuilder -> shellyConfig.getStatusUri(uriBuilder, heaterType, roomName))
            .retrieve()
            .bodyToMono(ShellyPro4StatusResponse.class);
    }

    public Mono<ShellyProRelayResponse> controlHeaterActor(
        final HeaterType heaterType,
        final RoomName roomName,
        final boolean enable
    ) {
        return relayConfig.standaloneAddress(roomName, heaterType)
            .<Mono<ShellyProRelayResponse>>map(address -> standaloneHeaterClient.relay(address, STANDALONE_CHANNEL, enable)
                .map(relay -> ShellyProRelayResponse.builder().ison(ison(relay)).build()))
            .orElseGet(() -> pro4Control(heaterType, roomName, enable))
            .doOnError(throwable ->
                log.error(
                    "Error while controlling actor for room: {}, heater type: {}: {}",
                    roomName, heaterType, throwable.toString()
                ))
            .onErrorMap(throwable -> new BoilerException(
                "Error while controlling actor for room: " + roomName + ", heater type: " + heaterType, throwable));
    }

    //a plug that answers no state is no answer - never read as off
    private static Boolean ison(final Relay relay) {
        if (relay.getIson() == null) {
            throw new IllegalStateException("The standalone heater answered no state");
        }
        return relay.getIson();
    }

    private Mono<ShellyProRelayResponse> pro4Control(final HeaterType heaterType, final RoomName roomName,
                                                     final boolean enable) {
        return shellyWebClient.get()
            .uri(uriBuilder -> shellyConfig.getControlUri(uriBuilder, heaterType, roomName, enable))
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class);
    }

    public Mono<ShellyPro4StatusResponse> getFloorPumpStatus() {
        return shellyWebClient.get()
            .uri(shellyConfig::getHeatingPumpStatusUri)
            .retrieve()
            .bodyToMono(ShellyPro4StatusResponse.class)
            .doOnError(throwable ->
                log.error("Error while getting floor pump status: {}", throwable.toString()))
            .onErrorMap(throwable -> new BoilerException("Error while getting floor pump status", throwable));
    }

    public Mono<ShellyProRelayResponse> controlFloorPump(final boolean enable) {
        return shellyWebClient.get()
            .uri(uriBuilder -> shellyConfig.getHeatingPumpControlUri(uriBuilder, enable))
            .retrieve()
            .bodyToMono(ShellyProRelayResponse.class)
            .doOnError(throwable ->
                log.error("Error while controlling floor pump: {}", throwable.toString()))
            .onErrorMap(throwable -> new BoilerException("Error while controlling floor pump", throwable));
    }
}
