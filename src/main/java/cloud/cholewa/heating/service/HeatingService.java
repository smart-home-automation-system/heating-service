package cloud.cholewa.heating.service;

import cloud.cholewa.heating.client.ShellyClient;
import cloud.cholewa.heating.model.HeaterActor;
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
        return Mono.fromRunnable(() -> homeStatus.setHomeHeatingEnabled(enabled));
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
        return Mono.fromSupplier(() -> isStatusStale(heaterActor))
            .filter(Boolean::booleanValue)
            .flatMap(refreshNeeded -> shellyClient.getHeaterActorStatus(heaterActor.getType(), roomName))
            .doOnNext(statusResponse -> updateHeaterStatus(statusResponse, heaterActor))
            .then(controlHeaterActor(heaterActor, roomName))
            .thenReturn(heaterActor);
    }

    private boolean isStatusStale(final HeaterActor heaterActor) {
        LocalDateTime lastUpdate = heaterActor.getLastStatusUpdate() == null
            ? LocalDateTime.MIN
            : heaterActor.getLastStatusUpdate();

        return LocalDateTime.now(clock).minusMinutes(5).isAfter(lastUpdate);
    }

    private void updateHeaterStatus(final ShellyPro4StatusResponse statusResponse, final HeaterActor heaterActor) {
        boolean isOn = Boolean.TRUE.equals(statusResponse.getOutput());
        log.info("Heater actor {} status updated: {}", heaterActor.getType(), isOn);
        heaterActor.setWorking(isOn);
        heaterActor.setLastStatusUpdate(LocalDateTime.now(clock));
    }

    private Mono<ShellyProRelayResponse> controlHeaterActor(final HeaterActor heaterActor, final RoomName roomName) {
        return Mono.defer(() -> {

            if (!homeStatus.isHomeHeatingEnabled() || !heaterActor.isInSchedule()) {
                if (heaterActor.isWorking()) {
                    return setHeaterActor(heaterActor, roomName, false);
                }
                return Mono.empty();
            }

            if (!heaterActor.isWorking()) {
                return setHeaterActor(heaterActor, roomName, true);
            }

            return Mono.empty();
        });
    }

    private Mono<ShellyProRelayResponse> setHeaterActor(
        final HeaterActor heaterActor,
        final RoomName roomName,
        final boolean enabled
    ) {
        return shellyClient.controlHeaterActor(heaterActor.getType(), roomName, enabled)
            .doOnNext(response -> {
                log.info(
                    "Switching heater actor {} for room {} to {}",
                    heaterActor.getType(),
                    roomName,
                    Boolean.TRUE.equals(response.getIson())
                );
                heaterActor.setWorking(Boolean.TRUE.equals(response.getIson()));
                heaterActor.setLastStatusUpdate(LocalDateTime.now(clock));
            });
    }
}
