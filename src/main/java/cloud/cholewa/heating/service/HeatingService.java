package cloud.cholewa.heating.service;

import cloud.cholewa.heating.client.ShellyClient;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.heating.model.HomeStatus;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.home.model.SystemActiveReply;
import cloud.cholewa.shelly.model.ShellyPro4StatusResponse;
import cloud.cholewa.shelly.model.ShellyProRelayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeatingService {

    private final Clock clock;
    private final HomeStatus homeStatus;
    private final ShellyClient shellyClient;

    public Mono<SystemActiveReply> queryHeatingSystemActive() {
        return Mono.just(SystemActiveReply.builder().active(homeStatus.isHomeHeatingEnabled()).build());
    }

    public Mono<Void> updateHomeStatus(final boolean enabled) {
        homeStatus.setHomeHeatingEnabled(enabled);
        return Mono.empty();
    }

    public Mono<Room> processHeatingRequest(final Room room) {
        return Flux.fromIterable(room.getHeaterActors())
            .flatMap(heaterActor -> handleHeaterActor(room.getName(), heaterActor))
            .mapNotNull(HeaterActor::isWorking)
            .reduce(Boolean::logicalOr)
            .map(heatingEnabled -> {
                room.setRoomHeatingEnabled(heatingEnabled);
                return room;
            });
    }

    /*
     * 1. check last update time
     * 2. if more than 5 minutes, query shelly and then update status and timestamp
     * 3. if the status is different from expected, control heater actor
     * 4. taking a response update status and timestamp
     * 5. return room
     * */
    private Mono<HeaterActor> handleHeaterActor(final RoomName roomName, final HeaterActor heaterActor) {
        return olderThanFiveMinutes(heaterActor.getLastStatusUpdate())
            .filter(needsUpdate -> needsUpdate)
            .flatMap(needsUpdate -> shellyClient.getHeaterActorStatus(heaterActor.getType(), roomName))
            .flatMap(statusResponse -> controlHeaterActor(heaterActor, statusResponse, roomName))
            .doOnNext(relayResponse -> updateHeaterStatus(relayResponse, heaterActor))
            .thenReturn(heaterActor);
    }

    private Mono<Boolean> olderThanFiveMinutes(final LocalDateTime lastUpdate) {
        return Mono.just(LocalDateTime.now(clock).minusMinutes(5).isAfter(lastUpdate));
    }

    private Mono<ShellyProRelayResponse> controlHeaterActor(
        final HeaterActor heaterActor,
        final ShellyPro4StatusResponse shellyResponse,
        final RoomName roomName
    ) {
        return Mono.defer(() -> {
            if (Boolean.TRUE.equals(shellyResponse.getOutput()) && !homeStatus.isHomeHeatingEnabled()) {
                return controlHeaterActor(heaterActor.getType(), roomName, false);
            } else if (Boolean.TRUE.equals(shellyResponse.getOutput()) != heaterActor.isWorking()) {
                return controlHeaterActor(heaterActor.getType(), roomName, shellyResponse.getOutput());
            } else {
                return Mono.empty();
            }
        });
    }

    private Mono<ShellyProRelayResponse> controlHeaterActor(
        final HeaterType heaterType,
        final RoomName roomName,
        final Boolean enabled
    ) {
        if (homeStatus.isHomeHeatingEnabled()) {
            log.info("Switching heater actor {} for room {} to status {}", heaterType, roomName, enabled);
            return shellyClient.controlHeaterActor(heaterType, roomName, enabled);
        } else {
            log.info("Disabling heater actor {} for room {}", heaterType, roomName);
            return shellyClient.controlHeaterActor(heaterType, roomName, false);
        }
    }

    private void updateHeaterStatus(final ShellyProRelayResponse relayResponse, final HeaterActor heaterActor) {
        log.info("Heater actor {} status updated: {}", heaterActor.getType(), heaterActor.isWorking());
        heaterActor.setWorking(Boolean.TRUE.equals(relayResponse.getIson()));
        heaterActor.setLastStatusUpdate(LocalDateTime.now(clock));
    }
}
