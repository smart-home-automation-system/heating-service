package cloud.cholewa.heating.shelly.pro.service;

import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.PumpType;
import cloud.cholewa.heating.shelly.pro.client.ShellyProClient;
import cloud.cholewa.shelly.model.ShellyProRelayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShellyProService {

    private static final double FIREPLACE_MIN_TEMPERATURE = 45;
    private static final double FIREPLACE_MAX_TEMPERATURE = 75;
    private final ShellyProClient client;
    private final Home home;

    public Mono<Boolean> controlFirePlacePump(final double fireplaceTemperature) {
        return client.controlFireplacePump(fireplaceTemperature > FIREPLACE_MIN_TEMPERATURE)
            .doOnNext(response -> {
                    home.getBoiler().getPumps().stream()
                        .filter(pump -> pump.getType().equals(PumpType.FIREPLACE_PUMP))
                        .findFirst().ifPresent(pump -> pump.setRunning(Boolean.TRUE.equals(response.getIson())));

                    log.info(
                        "Update fireplace pump status. Temperature is {} and pump status: {}",
                        fireplaceTemperature,
                        response.getIson()
                    );
                }
            )
            .map(Objects.requireNonNull(ShellyProRelayResponse::getIson));
    }
}
