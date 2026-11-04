package cloud.cholewa.heating.window;

import cloud.cholewa.commons.notification.NotificationPublisher;
import cloud.cholewa.heating.config.WindowProperties;
import cloud.cholewa.heating.db.model.OpeningSensorOpenAlertEntity;
import cloud.cholewa.heating.db.repository.OpeningSensorOpenAlertRepository;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.HomeStatus;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.home.model.RoomName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

//A window sensor that may be stuck open (HAS-350). A BLU Door/Window sends its window state only on a
//change: a lost "closed" packet leaves its room read open until the window moves again, and the hold of
//HAS-232 lasts for good (2026-10-30: four sensors, held since 17 - 26.10, all closed). The hold stays as it
//is, by the owner's choice, and the household is told once, only when the hold costs heat - when at a
//pass all three of these hold:
//- the sensor reads open and its gateway has heard no new window value from it for the time of
//  heating.window.stuck-open-after, measured by the stateAt of an open sensor and never by its last packet,
//  since the battery beacons keep coming while the window value is frozen
//- the hold of its room is on, as RoomWindow says it
//- the room would heat without the hold: the heating of the house is on and a heater of the room is in
//  schedule, which the control loop decides with the temperature below the target. Above the target
//  nothing is raised, so a door left open all summer says nothing
//One message per sensor and open period and no reminder, kept across a restart by the table
//opening_sensor_open_alert of V6. The row goes with one info once the gateway stands for a newer window
//value - closed, or an open heard later - and without a message when the sensor is switched off, gone from
//the registry or in a room this service does not heat. A room that warms above the target keeps its row and
//says nothing. A sensor whose gateway has gone silent keeps its row, and OpeningSensorMonitor tells that
//silence. The message goes first and the row second, each sensor guarded, as OpeningSensorMonitor does it.
//The alert is on the alert queue at the level warn, through publishReminder, the one warn-level alert of the
//library, since it is not a failure of a device. blinds-service keeps its copy of WindowSensors without this
//alert, or every message would come twice
@Slf4j
@Service
@RequiredArgsConstructor
public class StuckOpenMonitor {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Clock clock;
    private final Home home;
    private final HomeStatus homeStatus;
    private final WindowSensors windowSensors;
    private final WindowProperties properties;
    private final OpeningSensorOpenAlertRepository alertRepository;
    private final NotificationPublisher notificationPublisher;

    public Mono<Void> check() {
        //deferred: Spring subscribes again to the same Mono for every run - "now" is read for each
        return Mono.defer(() -> {
            final LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
            final Map<RoomName, Room> rooms = home.rooms().stream()
                .collect(Collectors.toMap(Room::getName, Function.identity(), (a, b) -> a));
            final Map<Long, WindowSensors.OpenSensor> open = windowSensors.open(rooms.keySet(), now).stream()
                .collect(Collectors.toMap(WindowSensors.OpenSensor::id, Function.identity()));

            return alertRepository.findAll()
                .collectMap(OpeningSensorOpenAlertEntity::sensorId)
                .flatMap(alerts -> Flux.concat(
                        Flux.fromIterable(open.values())
                            .filter(sensor -> !alerts.containsKey(sensor.id()))
                            .filter(sensor -> stuck(sensor, rooms.get(sensor.room()), now))
                            .concatMap(sensor -> guarded(sensor.id(), raise(sensor, now))),
                        Flux.fromIterable(alerts.values())
                            .concatMap(alert -> guarded(alert.sensorId(), close(alert, rooms.keySet(), now))))
                    .then());
        }).onErrorResume(throwable -> {
            //the rows could not be read: nothing is told this pass, the next one reads again
            log.error("Error while checking the window sensors that may be stuck open", throwable);
            return Mono.empty();
        });
    }

    private boolean stuck(final WindowSensors.OpenSensor sensor, final Room room, final LocalDateTime now) {
        return !sensor.stateAt().plus(properties.stuckOpenAfter()).isAfter(now)
            && windowSensors.windowOf(sensor.room(), now).hold()
            && wouldHeat(room);
    }

    //the heating of the house on and a heater of the room in schedule, as the control loop decided it last
    private boolean wouldHeat(final Room room) {
        return homeStatus.isEnabledHomeHeatingSystem()
            && room != null && room.getHeaterActors() != null
            && room.getHeaterActors().stream().anyMatch(HeaterActor::isInSchedule);
    }

    //one failing sensor must not hide the others
    private Mono<Void> guarded(final long sensor, final Mono<Void> step) {
        return step.onErrorResume(throwable -> {
            log.error("Error while checking whether window sensor {} is stuck open", sensor, throwable);
            return Mono.empty();
        });
    }

    private Mono<Void> raise(final WindowSensors.OpenSensor sensor, final LocalDateTime now) {
        return notificationPublisher.publishReminder(stuckMessage(sensor))
            .then(Mono.defer(() -> alertRepository.save(
                new OpeningSensorOpenAlertEntity(null, sensor.id(), sensor.openSince(), sensor.stateAt(), now))))
            .then();
    }

    //A newer window value ends it: closed, or an open the gateway heard after the one told. Not known keeps
    //it. Switched off, gone, or in a room this service does not heat: the row goes without a message
    private Mono<Void> close(
        final OpeningSensorOpenAlertEntity alert,
        final Set<RoomName> rooms,
        final LocalDateTime now
    ) {
        return windowSensors.describe(alert.sensorId())
            .filter(described -> rooms.contains(described.room()))
            .map(sensor -> windowSensors.valueOf(alert.sensorId(), now)
                .filter(value -> !value.open() || value.stateAt().isAfter(alert.stateUpdatedAt()))
                .map(value -> notificationPublisher.publishInfo("Window sensor %s in room %s reports again: %s"
                        .formatted(sensor.name(), sensor.room().getValue(), value.open() ? "open" : "closed"))
                    .then(Mono.defer(() -> alertRepository.delete(alert))))
                .orElseGet(Mono::empty))
            .orElseGet(() -> alertRepository.delete(alert));
    }

    private static String stuckMessage(final WindowSensors.OpenSensor sensor) {
        return ("Window sensor %s in room %s has read open since %s without a change; the heating of the room is "
            + "held. If it is closed, open and close it once - a lost 'closed' packet")
            .formatted(sensor.name(), sensor.room().getValue(), DATE_FORMAT.format(sensor.openSince()));
    }
}
