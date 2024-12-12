package cloud.cholewa.heating.room.service;

import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.shelly.actor.HeaterPro4Client;
import cloud.cholewa.heating.shelly.actor.HeaterPro4Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
class HeaterActorHandler {

    private final HeaterPro4Client heaterPro4Client;

    Mono<Void> turnOnHeaterActor(
        final Room room,
        final HeaterActor heaterActor,
        final HeaterPro4Config.HeaterPro4Data heaterData,
        final String reasonMessage
    ) {
        if (!heaterActor.isWorking()) {
            return heaterPro4Client.controlHeatingActor(heaterData, true)
                .then(heaterPro4Client.getHeaterActorStatus(heaterData))
                .doOnNext(response -> heaterActor.setWorking(Boolean.TRUE.equals(response.getOutput())))
                .then(Mono.fromRunnable(() -> logStatus(room, heaterActor, reasonMessage)));
        }
        return Mono.empty();
    }

    Mono<Void> turnOffHeaterActor(
        final Room room,
        final HeaterActor heaterActor,
        final HeaterPro4Config.HeaterPro4Data heaterData
    ) {
        return heaterPro4Client.getHeaterActorStatus(heaterData)
            .doOnNext(response -> heaterActor.setWorking(Boolean.TRUE.equals(response.getOutput())))
            .then(heaterActor.isWorking() ? heaterPro4Client.controlHeatingActor(heaterData, false) : Mono.empty())
            .then(heaterActor.isWorking() ? heaterPro4Client.getHeaterActorStatus(heaterData) : Mono.empty())
            .doOnNext(response -> {
                if (heaterActor.isWorking()) {
                    logStatus(room, heaterActor, "turned off");
                    heaterActor.setWorking(Boolean.FALSE.equals(response.getOutput()));
                }
            })
            .then();
    }

    private void logStatus(final Room room, final HeaterActor heaterActor, final String reasonMessage) {
        log.info(
            "Heater status, room: [{}], current temp: [{}] heat source: [{}], isWorking: [{}], reason: [{}]",
            room.getName().name(),
            room.getTemperature().getValue(),
            heaterActor.getName(),
            heaterActor.isWorking(),
            reasonMessage
        );
    }
}
