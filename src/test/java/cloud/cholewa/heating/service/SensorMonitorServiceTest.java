package cloud.cholewa.heating.service;

import cloud.cholewa.heating.config.SensorMonitorProperties;
import cloud.cholewa.heating.db.model.TemperatureSensorAlertEntity;
import cloud.cholewa.heating.db.repository.TemperatureSensorAlertRepository;
import cloud.cholewa.heating.model.TemperatureSensorReply;
import cloud.cholewa.heating.rabbit.NotificationPublisher;
import cloud.cholewa.home.model.RoomName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorMonitorServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 12, 0);
    private static final LocalDateTime LAST_READING = LocalDateTime.of(2026, 10, 4, 10, 30);
    private static final String SILENT_MESSAGE =
        "Temperature sensor in room office is not reporting (last reading 2026-10-04 10:30)";

    private final MovableClock clock = new MovableClock(NOW);

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private TemperatureSensorService temperatureSensorService;
    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private TemperatureSensorAlertRepository alertRepository;
    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private NotificationPublisher notificationPublisher;

    private SensorMonitorService sut;

    @BeforeEach
    void setUp() {
        sut = new SensorMonitorService(
            clock,
            new SensorMonitorProperties("0 0 * * * *", Duration.ofHours(24), Duration.ofHours(24), Set.of()),
            temperatureSensorService,
            alertRepository,
            notificationPublisher
        );
    }

    @Test
    void should_raise_alert_once_when_sensor_becomes_stale() {
        when(temperatureSensorService.queryReadableSensors()).thenReturn(Flux.just(sensor(true)));
        when(alertRepository.findAll()).thenReturn(Flux.empty());
        when(notificationPublisher.publishAlert(SILENT_MESSAGE)).thenReturn(Mono.empty());
        when(alertRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        sut.checkSensors().as(StepVerifier::create).verifyComplete();

        verify(alertRepository).save(new TemperatureSensorAlertEntity(null, "office", NOW, NOW));
    }

    @Test
    void should_stay_quiet_when_stale_sensor_was_reported_less_than_reminder_interval_ago() {
        when(temperatureSensorService.queryReadableSensors()).thenReturn(Flux.just(sensor(true)));
        when(alertRepository.findAll()).thenReturn(Flux.just(
            new TemperatureSensorAlertEntity(7L, "office", NOW.minusHours(23), NOW.minusHours(23))));

        sut.checkSensors().as(StepVerifier::create).verifyComplete();

        verify(notificationPublisher, never()).publishAlert(anyString());
        verify(alertRepository, never()).save(any());
    }

    @Test
    void should_remind_when_sensor_is_still_stale_after_reminder_interval() {
        final LocalDateTime staleSince = NOW.minusHours(24);

        when(temperatureSensorService.queryReadableSensors()).thenReturn(Flux.just(sensor(true)));
        when(alertRepository.findAll()).thenReturn(Flux.just(
            new TemperatureSensorAlertEntity(7L, "office", staleSince, staleSince)));
        when(notificationPublisher.publishReminder(SILENT_MESSAGE)).thenReturn(Mono.empty());
        when(alertRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        sut.checkSensors().as(StepVerifier::create).verifyComplete();

        //the same row moves on: stale since stays, only the last alert is new
        verify(alertRepository).save(new TemperatureSensorAlertEntity(7L, "office", staleSince, NOW));
        //a reminder, not a second alert: it is shown in another color
        verify(notificationPublisher, never()).publishAlert(anyString());
    }

    //the pass a day later starts a few milliseconds earlier within its second than the one that
    //raised the alert; compared exactly, the reminder would wait for the next hour
    @Test
    void should_remind_after_exactly_one_interval_despite_scheduler_jitter() {
        clock.moveTo(NOW.plusNanos(4_000_000));
        when(temperatureSensorService.queryReadableSensors()).thenReturn(Flux.just(sensor(true)));
        when(alertRepository.findAll()).thenReturn(Flux.empty());
        when(notificationPublisher.publishAlert(SILENT_MESSAGE)).thenReturn(Mono.empty());
        when(alertRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        sut.checkSensors().as(StepVerifier::create).verifyComplete();
        verify(alertRepository).save(new TemperatureSensorAlertEntity(null, "office", NOW, NOW));

        clock.moveTo(NOW.plusHours(24).plusNanos(1_000_000));
        when(alertRepository.findAll()).thenReturn(Flux.just(
            new TemperatureSensorAlertEntity(7L, "office", NOW, NOW)));
        when(notificationPublisher.publishReminder(SILENT_MESSAGE)).thenReturn(Mono.empty());

        sut.checkSensors().as(StepVerifier::create).verifyComplete();

        verify(notificationPublisher).publishAlert(SILENT_MESSAGE);
        verify(notificationPublisher).publishReminder(SILENT_MESSAGE);
        verify(alertRepository).save(new TemperatureSensorAlertEntity(7L, "office", NOW, NOW.plusHours(24)));
    }

    @Test
    void should_publish_info_and_forget_alert_when_sensor_reports_again() {
        final TemperatureSensorAlertEntity alert =
            new TemperatureSensorAlertEntity(7L, "office", NOW.minusDays(2), NOW.minusHours(3));

        when(temperatureSensorService.queryReadableSensors()).thenReturn(Flux.just(sensor(false)));
        when(alertRepository.findAll()).thenReturn(Flux.just(alert));
        when(notificationPublisher.publishInfo(
            "Temperature sensor in room office is reporting again (last reading 2026-10-04 10:30)"))
            .thenReturn(Mono.empty());
        when(alertRepository.delete(alert)).thenReturn(Mono.empty());

        sut.checkSensors().as(StepVerifier::create).verifyComplete();

        verify(alertRepository).delete(alert);
        verify(notificationPublisher, never()).publishAlert(anyString());
    }

    @Test
    void should_keep_alert_when_recovery_info_was_not_published() {
        final TemperatureSensorAlertEntity alert =
            new TemperatureSensorAlertEntity(7L, "office", NOW.minusDays(2), NOW.minusHours(3));

        when(temperatureSensorService.queryReadableSensors()).thenReturn(Flux.just(sensor(false)));
        when(alertRepository.findAll()).thenReturn(Flux.just(alert));
        when(notificationPublisher.publishInfo(anyString()))
            .thenReturn(Mono.error(new IllegalStateException("not routed")));
        //assembled eagerly by then(...), but must never be subscribed
        when(alertRepository.delete(alert)).thenReturn(Mono.error(new AssertionError("alert forgotten")));

        //the row stays, so the next pass tries the info again
        sut.checkSensors().as(StepVerifier::create).verifyComplete();
    }

    @Test
    void should_do_nothing_for_reporting_sensor_without_alert() {
        when(temperatureSensorService.queryReadableSensors()).thenReturn(Flux.just(sensor(false)));
        when(alertRepository.findAll()).thenReturn(Flux.empty());

        sut.checkSensors().as(StepVerifier::create).verifyComplete();

        verify(notificationPublisher, never()).publishAlert(anyString());
        verify(notificationPublisher, never()).publishInfo(anyString());
    }

    @Test
    void should_skip_muted_sensor_even_when_stale() {
        when(temperatureSensorService.queryReadableSensors()).thenReturn(Flux.just(
            new TemperatureSensorReply(RoomName.SAUNA, LAST_READING, true, true)));
        when(alertRepository.findAll()).thenReturn(Flux.empty());

        sut.checkSensors().as(StepVerifier::create).verifyComplete();

        verify(notificationPublisher, never()).publishAlert(anyString());
        verify(alertRepository, never()).save(any());
    }

    @Test
    void should_forget_alert_of_muted_sensor_without_any_notification() {
        final TemperatureSensorAlertEntity alert =
            new TemperatureSensorAlertEntity(9L, "sauna", NOW.minusDays(30), NOW.minusHours(5));

        when(temperatureSensorService.queryReadableSensors()).thenReturn(Flux.just(
            new TemperatureSensorReply(RoomName.SAUNA, LAST_READING, false, true)));
        when(alertRepository.findAll()).thenReturn(Flux.just(alert));
        when(alertRepository.delete(alert)).thenReturn(Mono.empty());

        sut.checkSensors().as(StepVerifier::create).verifyComplete();

        verify(alertRepository).delete(alert);
        verify(notificationPublisher, never()).publishInfo(anyString());
        verify(notificationPublisher, never()).publishAlert(anyString());
    }

    @Test
    void should_not_store_alert_when_notification_was_not_published() {
        when(temperatureSensorService.queryReadableSensors()).thenReturn(Flux.just(sensor(true)));
        when(alertRepository.findAll()).thenReturn(Flux.empty());
        when(notificationPublisher.publishAlert(SILENT_MESSAGE))
            .thenReturn(Mono.error(new IllegalStateException("broker down")));
        //assembled eagerly by then(...), but must never be subscribed
        when(alertRepository.save(any())).thenReturn(Mono.error(new AssertionError("alert stored")));

        //the next pass finds no row and raises the alert again
        sut.checkSensors().as(StepVerifier::create).verifyComplete();
    }

    @Test
    void should_check_remaining_sensors_when_one_fails() {
        final TemperatureSensorReply garage = new TemperatureSensorReply(RoomName.GARAGE, LAST_READING, true, false);

        when(temperatureSensorService.queryReadableSensors()).thenReturn(Flux.just(sensor(true), garage));
        when(alertRepository.findAll()).thenReturn(Flux.empty());
        when(notificationPublisher.publishAlert(SILENT_MESSAGE))
            .thenReturn(Mono.error(new IllegalStateException("broker down")));
        when(notificationPublisher.publishAlert(
            "Temperature sensor in room garage is not reporting (last reading 2026-10-04 10:30)"))
            .thenReturn(Mono.empty());
        when(alertRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        sut.checkSensors().as(StepVerifier::create).verifyComplete();

        verify(alertRepository).save(new TemperatureSensorAlertEntity(null, "garage", NOW, NOW));
    }

    //Spring calls a reactive @Scheduled method once and subscribes to the returned Mono for every
    //run - a fresh call per run would hide a "now" frozen when the Mono was built
    @Test
    void should_read_the_clock_on_every_subscription_of_the_same_mono() {
        when(temperatureSensorService.queryReadableSensors()).thenReturn(Flux.just(sensor(true)));
        when(alertRepository.findAll()).thenReturn(Flux.empty());
        when(notificationPublisher.publishAlert(SILENT_MESSAGE)).thenReturn(Mono.empty());
        when(alertRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        final Mono<Void> scheduled = sut.checkSensors();

        scheduled.as(StepVerifier::create).verifyComplete();
        clock.moveTo(NOW.plusHours(1));
        scheduled.as(StepVerifier::create).verifyComplete();

        verify(alertRepository).save(new TemperatureSensorAlertEntity(null, "office", NOW, NOW));
        verify(alertRepository).save(
            new TemperatureSensorAlertEntity(null, "office", NOW.plusHours(1), NOW.plusHours(1)));
    }

    private static TemperatureSensorReply sensor(final boolean stale) {
        return new TemperatureSensorReply(RoomName.OFFICE, LAST_READING, stale, false);
    }

    //a Clock mock cannot be moved between two subscriptions without re-stubbing, and Mockito
    //refuses smart nulls for the sealed ZoneId
    private static final class MovableClock extends Clock {

        private Instant instant;

        private MovableClock(final LocalDateTime now) {
            moveTo(now);
        }

        private void moveTo(final LocalDateTime now) {
            instant = now.atZone(ZONE).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return ZONE;
        }

        @Override
        public Clock withZone(final ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
