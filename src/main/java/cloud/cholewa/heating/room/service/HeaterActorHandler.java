package cloud.cholewa.heating.room.service;

import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.shelly.actor.HeaterPro4Client;
import cloud.cholewa.heating.shelly.actor.HeaterPro4Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
class HeaterActorHandler {

    private final HeaterPro4Client heaterPro4Client;

    Mono<HeaterActor> getStatus(final HeaterActor heaterActor, final HeaterPro4Config.HeaterPro4Data heaterData) {
        return heaterPro4Client.getHeaterActorStatus(heaterData)
            .doOnNext(response -> heaterActor.setWorking(Boolean.TRUE.equals(response.getOutput())))
            .then(Mono.just(heaterActor));
    }

    Mono<Void> turnOnHeaterActor(
        final Room room,
        final HeaterActor heaterActor,
        final HeaterPro4Config.HeaterPro4Data heaterData,
        final String reasonMessage
    ) {
        return heaterPro4Client.getHeaterActorStatus(heaterData)
            .doOnNext(response -> heaterActor.setWorking(Boolean.TRUE.equals(response.getOutput())))
            .then(!heaterActor.isWorking() ? heaterPro4Client.setHeaterActor(heaterData, true) : Mono.empty())
            .delayElement(Duration.ofSeconds(1))
            .flatMap(response -> heaterPro4Client.getHeaterActorStatus(heaterData))
            .doOnNext(response -> {
                heaterActor.setWorking(Boolean.TRUE.equals(response.getOutput()));
                logStatus(room, heaterActor, reasonMessage);
            })
            .then();
    }

    Mono<Void> turnOffHeaterActor(
        final Room room,
        final HeaterActor heaterActor,
        final HeaterPro4Config.HeaterPro4Data heaterData
    ) {
        return heaterPro4Client.getHeaterActorStatus(heaterData)
            .doOnNext(response -> heaterActor.setWorking(Boolean.TRUE.equals(response.getOutput())))
            .then(heaterActor.isWorking() ? heaterPro4Client.setHeaterActor(heaterData, false) : Mono.empty())
            .delayElement(Duration.ofSeconds(1))
            .flatMap(response ->  heaterPro4Client.getHeaterActorStatus(heaterData))
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
