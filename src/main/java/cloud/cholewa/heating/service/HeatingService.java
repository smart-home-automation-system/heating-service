package cloud.cholewa.heating.service;

import cloud.cholewa.heating.client.ShellyClient;
import cloud.cholewa.heating.infrastructure.error.BoilerException;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.HomeStatus;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.window.RoomWindow;
import cloud.cholewa.heating.window.WindowSensors;
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
    private final WindowSensors windowSensors;

    public Mono<SystemActiveReply> queryHeatingSystemEnabledAndActive() {
        return Mono.just(SystemActiveReply.builder()
            .active(homeStatus.isAnyHeaterActive() && homeStatus.isEnabledHomeHeatingSystem())
            .build());
    }

    //The windows of the room are judged once for the pass, so that every heater of it follows the same
    //decision (HAS-232): a window or a door open for heating.window.hold-after holds them all, the
    //floor included. Only at a reading of the room - the hold starts and ends with the next one
    public Mono<Room> processHeatingRequest(final Room room) {
        return Mono.fromSupplier(() -> windowSensors.windowOf(room.getName()))
            .doOnNext(window -> logHold(room.getName(), window))
            .flatMapMany(window -> Flux.fromIterable(room.getHeaterActors())
                .flatMap(heaterActor -> handleHeaterActor(room.getName(), heaterActor, window.hold())))
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
    private void logHold(final RoomName roomName, final RoomWindow window) {
        if (window.hold()) {
            log.info("Heating of room: {} is held, a window or a door is open since {}", roomName.getValue(), window.openSince());
        }
    }

    private Mono<HeaterActor> handleHeaterActor(final RoomName roomName, final HeaterActor heaterActor, final boolean held) {
        return Mono.fromSupplier(() -> isStatusStale(heaterActor))
            .filter(Boolean::booleanValue)
            .flatMap(refreshNeeded -> shellyClient.getHeaterActorStatus(heaterActor.getType(), roomName))
            .doOnNext(statusResponse -> updateHeaterStatus(roomName, statusResponse, heaterActor))
            .then(controlHeaterActor(heaterActor, roomName, held))
            .thenReturn(heaterActor)
            //a device that fails - since the calls have a timeout, also one that is merely slow -
            //must not cancel the other actors of the room nor end the pass before the furnace and
            //the floor pump are decided. The state is written only from a device response, so this
            //actor keeps what it last reported. Only the device failure is skipped (ShellyClient
            //has logged it at ERROR): anything else, a room missing from the configuration
            //included, still ends the pass
            .onErrorResume(BoilerException.class, exception -> {
                log.warn(
                    "Skipping heater actor: {} in room: {}: {}",
                    heaterActor.getType(), roomName, String.valueOf(exception.getCause())
                );
                return Mono.just(heaterActor);
            });
    }

    private boolean isStatusStale(final HeaterActor heaterActor) {
        LocalDateTime lastUpdate = heaterActor.getLastStatusUpdate() == null
            ? LocalDateTime.MIN
            : heaterActor.getLastStatusUpdate();

        return LocalDateTime.now(clock).minusMinutes(5).isAfter(lastUpdate);
    }

    private void updateHeaterStatus(
        final RoomName roomName,
        final ShellyPro4StatusResponse statusResponse,
        final HeaterActor heaterActor
    ) {
        boolean isOn = Boolean.TRUE.equals(statusResponse.getOutput());

        log.info(
            "Heater status was stale. Updated room: {}, actor: {}, status: {}",
            roomName.getValue(),
            heaterActor.getType(),
            isOn ? "on" : "off"
        );

        heaterActor.setWorking(isOn);
        heaterActor.setLastStatusUpdate(LocalDateTime.now(clock));
    }

    private Mono<ShellyProRelayResponse> controlHeaterActor(
        final HeaterActor heaterActor,
        final RoomName roomName,
        final boolean held
    ) {
        return Mono.defer(() -> {

            if (!homeStatus.isEnabledHomeHeatingSystem() || !heaterActor.isInSchedule() || held) {
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
                    "Switching heater actor: {} in room: {} to: {}",
                    heaterActor.getType(),
                    roomName,
                    Boolean.TRUE.equals(response.getIson()) ? "on" : "off"
                );
                heaterActor.setWorking(Boolean.TRUE.equals(response.getIson()));
                heaterActor.setLastStatusUpdate(LocalDateTime.now(clock));
            });
    }
}
