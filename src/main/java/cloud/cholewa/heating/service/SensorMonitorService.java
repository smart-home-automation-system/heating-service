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
import java.util.Optional;

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
     * reminder interval, a sensor that came back is reported once.
     */
    public Mono<Void> checkSensors() {
        //deferred: Spring calls a reactive @Scheduled method once and subscribes to the same Mono
        //for every run, so "now" read outside the chain would stay at the start of the service
        return Mono.defer(() -> {
            final LocalDateTime now = LocalDateTime.now(clock);

            return temperatureSensorService.querySensors()
                .concatMap(sensor -> checkSensor(sensor, now)
                    //one failing sensor must not hide the others
                    .onErrorResume(throwable -> {
                        log.error("Error while checking temperature sensor of room: {}", sensor.room(), throwable);
                        return Mono.empty();
                    }))
                .then();
        });
    }

    private Mono<Void> checkSensor(final TemperatureSensorReply sensor, final LocalDateTime now) {
        return alertRepository.findByRoom(sensor.room().getValue())
            .map(Optional::of)
            .defaultIfEmpty(Optional.empty())
            .flatMap(alert -> {
                if (sensor.stale()) {
                    return alert
                        .map(existing -> remind(sensor, existing, now))
                        .orElseGet(() -> raise(sensor, now));
                }
                return alert
                    .map(existing -> recover(sensor, existing))
                    .orElseGet(Mono::empty);
            });
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
