package cloud.cholewa.heating.service;

import cloud.cholewa.heating.config.SensorMonitorProperties;
import cloud.cholewa.heating.db.model.TemperatureSensorAlertEntity;
import cloud.cholewa.heating.db.repository.TemperatureSensorAlertRepository;
import cloud.cholewa.heating.model.TemperatureSensorReply;
import cloud.cholewa.heating.rabbit.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorMonitorService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Clock clock;
    private final SensorMonitorProperties properties;
    private final TemperatureSensorService temperatureSensorService;
    private final TemperatureSensorAlertRepository alertRepository;
    private final NotificationPublisher notificationPublisher;

    /**
     * One pass over all sensors: a sensor that went silent is reported once and then again every
     * reminder interval, a sensor that came back is reported once. Muted rooms are skipped.
     */
    public Mono<Void> checkSensors() {
        //deferred: Spring calls a reactive @Scheduled method once and subscribes to the same Mono
        //for every run, so "now" read outside the chain would stay at the start of the service
        return Mono.defer(() -> {
            //to the minute: two passes a reminder interval apart start milliseconds off each
            //other, and compared exactly the reminder would be due or not by that jitter
            final LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);

            //the table holds only the sensors that are silent right now, so one read for the pass
            return alertRepository.findAll()
                .collectMap(TemperatureSensorAlertEntity::room)
                .flatMap(alerts -> checkSensors(alerts, now));
        });
    }

    private Mono<Void> checkSensors(final Map<String, TemperatureSensorAlertEntity> alerts, final LocalDateTime now) {
        return temperatureSensorService.queryReadableSensors()
            .concatMap(sensor -> checkSensor(sensor, alerts.get(sensor.room().getValue()), now)
                //one failing sensor must not hide the others
                .onErrorResume(throwable -> {
                    log.error("Error while checking temperature sensor of room: {}", sensor.room(), throwable);
                    return Mono.empty();
                }))
            .then();
    }

    private Mono<Void> checkSensor(
        final TemperatureSensorReply sensor,
        final TemperatureSensorAlertEntity alert,
        final LocalDateTime now
    ) {
        if (sensor.muted()) {
            //muted while it was silent: left in place, the row would produce a "reporting again"
            //message on the day the room is un-muted, about a recovery long past
            return alert == null ? Mono.empty() : alertRepository.delete(alert);
        }
        if (sensor.stale()) {
            return alert == null ? raise(sensor, now) : remind(sensor, alert, now);
        }
        return alert == null ? Mono.empty() : recover(sensor, alert);
    }

    //the notification goes first in all three: when the write that follows fails, the next pass
    //repeats the message, while the other order would lose it
    private Mono<Void> raise(final TemperatureSensorReply sensor, final LocalDateTime now) {
        return notificationPublisher.publishAlert(silentMessage(sensor))
            .then(alertRepository.save(
                new TemperatureSensorAlertEntity(null, sensor.room().getValue(), now, now)))
            .then();
    }

    private Mono<Void> remind(
        final TemperatureSensorReply sensor,
        final TemperatureSensorAlertEntity alert,
        final LocalDateTime now
    ) {
        if (alert.lastAlertAt().plus(properties.reminderInterval()).isAfter(now)) {
            return Mono.empty();
        }
        return notificationPublisher.publishAlert(silentMessage(sensor))
            .then(alertRepository.save(
                new TemperatureSensorAlertEntity(alert.id(), alert.room(), alert.staleSince(), now)))
            .then();
    }

    private Mono<Void> recover(final TemperatureSensorReply sensor, final TemperatureSensorAlertEntity alert) {
        return notificationPublisher.publishInfo(
                "Temperature sensor in room %s is reporting again (last reading %s)"
                    .formatted(sensor.room().getValue(), DATE_FORMAT.format(sensor.lastReadingAt())))
            .then(alertRepository.delete(alert));
    }

    private String silentMessage(final TemperatureSensorReply sensor) {
        return "Temperature sensor in room %s is not reporting (last reading %s)"
            .formatted(sensor.room().getValue(), DATE_FORMAT.format(sensor.lastReadingAt()));
    }
}
