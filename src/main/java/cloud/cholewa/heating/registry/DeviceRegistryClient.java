package cloud.cholewa.heating.registry;

import cloud.cholewa.commons.error.DownstreamErrors;
import cloud.cholewa.home.model.DeviceKind;
import cloud.cholewa.home.model.SmartDevice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

//The opening sensors and the BTHome gateways of the device registry of database-service (HAS-232),
//a copy of the client of blinds-service. What a failing database-service says goes to the log, never
//further; no body at all, or one that is not a list, is a failed call - never an empty registry,
//which would let every window of the house go
@Slf4j
@Component
public class DeviceRegistryClient {

    private static final ParameterizedTypeReference<List<SmartDevice>> DEVICES = new ParameterizedTypeReference<>() {
    };

    private final WebClient databaseWebClient;

    public DeviceRegistryClient(@Qualifier("databaseWebClient") final WebClient databaseWebClient) {
        this.databaseWebClient = databaseWebClient;
    }

    //every opening sensor, switched on or off - the window sensors decide what a switched-off one is
    public Mono<List<SmartDevice>> getOpeningSensors() {
        return devices(DeviceKind.OPENING_SENSOR, null);
    }

    public Mono<List<SmartDevice>> getBthomeGateways() {
        return devices(DeviceKind.BTHOME_GATEWAY, true);
    }

    private Mono<List<SmartDevice>> devices(final DeviceKind kind, final Boolean enabled) {
        return databaseWebClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .path("/home/device/registry")
                .queryParam("kind", kind.getValue())
                .queryParamIfPresent("enabled", Optional.ofNullable(enabled))
                .build()
            )
            .retrieve()
            .onStatus(HttpStatusCode::isError, response -> DownstreamErrors.read(response)
                .map(error -> {
                    log.warn("database-service answered {} to the read of the devices of the kind {}: {}",
                        error.status().value(), kind.getValue(), error.errors());
                    return new RegistryCallException("database-service answered " + error.status().value());
                }))
            .bodyToMono(DEVICES)
            .switchIfEmpty(Mono.error(() -> new RegistryCallException("database-service answered without a list")));
    }
}
