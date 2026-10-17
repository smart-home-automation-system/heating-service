package cloud.cholewa.heating.window;

import cloud.cholewa.commons.notification.NotificationPublisher;
import cloud.cholewa.heating.config.SensorMonitorProperties;
import cloud.cholewa.heating.db.model.OpeningSensorAlertEntity;
import cloud.cholewa.heating.db.repository.OpeningSensorAlertRepository;
import cloud.cholewa.heating.model.Home;
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

//The silent window and door sensors of the rooms this service heats (HAS-232). A silent sensor does
//not hold the heating of its room - the room heats as without it - so the household has to hear of
//it: reported once, again every heating.sensor-monitor.reminder-interval while it stays silent, and
//once more when it is back - the way the silent temperature sensors are (SensorMonitorService), the
//notification first and the row second, so that a failed write repeats the message rather than
//losing it. A sensor the registry no longer has switched on, or that moved to a room without
//heating, leaves its row without a message
@Slf4j
@Service
@RequiredArgsConstructor
public class OpeningSensorMonitor {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Clock clock;
    private final Home home;
    private final WindowSensors windowSensors;
    private final SensorMonitorProperties properties;
    private final OpeningSensorAlertRepository alertRepository;
    private final NotificationPublisher notificationPublisher;

    public Mono<Void> check() {
        //deferred: Spring calls a reactive @Scheduled method once and subscribes to the same Mono
        //for every run, so "now" read outside the chain would stay at the start of the service
        return Mono.defer(() -> {
            //to the minute, as in SensorMonitorService: two passes a reminder interval apart start
            //milliseconds off each other
            final LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
            final Set<RoomName> rooms = home.rooms().stream().map(Room::getName).collect(Collectors.toSet());
            final Map<Long, WindowSensors.SilentSensor> silent = windowSensors.silent(rooms, now).stream()
                .collect(Collectors.toMap(WindowSensors.SilentSensor::id, Function.identity()));

            return alertRepository.findAll()
                .collectMap(OpeningSensorAlertEntity::sensorId)
                .flatMap(alerts -> Flux.concat(
                        Flux.fromIterable(silent.values())
                            .concatMap(sensor -> guarded(sensor.id(), alerts.get(sensor.id()) == null
                                ? raise(sensor, now)
                                : remind(sensor, alerts.get(sensor.id()), now))),
                        Flux.fromIterable(alerts.values())
                            .filter(alert -> !silent.containsKey(alert.sensorId()))
                            .concatMap(alert -> guarded(alert.sensorId(), close(alert, rooms, now))))
                    .then());
        });
    }

    //one failing sensor must not hide the others. The writes are deferred: a message that did not get
    //through must not even ask for its row
    private Mono<Void> guarded(final long sensor, final Mono<Void> step) {
        return step.onErrorResume(throwable -> {
            log.error("Error while checking window sensor {}", sensor, throwable);
            return Mono.empty();
        });
    }

    private Mono<Void> raise(final WindowSensors.SilentSensor sensor, final LocalDateTime now) {
        final LocalDateTime since = sensor.lastStateAt() == null ? now : sensor.lastStateAt();
        return notificationPublisher.publishAlert(silentMessage(sensor))
            .then(Mono.defer(() -> alertRepository.save(new OpeningSensorAlertEntity(null, sensor.id(), since, now))))
            .then();
    }

    private Mono<Void> remind(
        final WindowSensors.SilentSensor sensor,
        final OpeningSensorAlertEntity alert,
        final LocalDateTime now
    ) {
        if (alert.lastAlertAt().plus(properties.reminderInterval()).isAfter(now)) {
            return Mono.empty();
        }
        return notificationPublisher.publishReminder(silentMessage(sensor))
            .then(Mono.defer(() -> alertRepository.save(
                new OpeningSensorAlertEntity(alert.id(), alert.sensorId(), alert.silentSince(), now))))
            .then();
    }

    //Back only when a gateway stands for a state of it again. Not silent and not known is the grace a
    //sensor has after a restart of the service, or while its gateway has only just stopped answering:
    //the row stays, or every rollout would announce a recovery and raise the alert again half an hour
    //later. Gone from the registry, switched off, or in a room this service does not heat is no
    //recovery to announce
    private Mono<Void> close(final OpeningSensorAlertEntity alert, final Set<RoomName> rooms, final LocalDateTime now) {
        return windowSensors.describe(alert.sensorId())
            .filter(sensor -> rooms.contains(sensor.room()))
            .map(sensor -> windowSensors.isKnown(sensor.id(), now)
                ? notificationPublisher.publishInfo("Window sensor %s in room %s is reporting again"
                        .formatted(sensor.name(), sensor.room().getValue()))
                    .then(Mono.defer(() -> alertRepository.delete(alert)))
                : Mono.<Void>empty())
            .orElseGet(() -> alertRepository.delete(alert));
    }

    private static String silentMessage(final WindowSensors.SilentSensor sensor) {
        return "Window sensor %s in room %s is not reporting (%s); the room heats as if it had no sensor"
            .formatted(sensor.name(), sensor.room().getValue(), sensor.lastStateAt() == null
                ? "no state since the start of the service"
                : "last state " + DATE_FORMAT.format(sensor.lastStateAt()));
    }
}
