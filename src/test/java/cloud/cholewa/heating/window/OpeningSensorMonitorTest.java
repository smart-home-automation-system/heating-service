package cloud.cholewa.heating.window;

import cloud.cholewa.commons.notification.NotificationPublisher;
import cloud.cholewa.heating.config.SensorMonitorProperties;
import cloud.cholewa.heating.db.model.OpeningSensorAlertEntity;
import cloud.cholewa.heating.db.repository.OpeningSensorAlertRepository;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Room;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//The silent window sensors of the heated rooms reported the way the silent temperature sensors are
//(HAS-232): once, a reminder a day, once more when back - the message first, the row second
@ExtendWith(MockitoExtension.class)
class OpeningSensorMonitorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 17, 21, 0);
    private static final Set<RoomName> HEATED = Set.of(RoomName.LIVING_ROOM, RoomName.BEDROOM);
    private static final WindowSensors.SilentSensor TERRACE_DOOR =
        new WindowSensors.SilentSensor(68, "Terrace door", RoomName.LIVING_ROOM, LocalDateTime.of(2026, 10, 17, 20, 15));
    private static final String SILENT_MESSAGE = "Window sensor Terrace door in room living room is not reporting "
        + "(last state 2026-10-17 20:15); the room heats as if it had no sensor";

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private WindowSensors windowSensors;
    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private OpeningSensorAlertRepository alertRepository;
    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private NotificationPublisher notificationPublisher;

    private OpeningSensorMonitor sut;

    @BeforeEach
    void setUp() {
        sut = new OpeningSensorMonitor(
            Clock.fixed(NOW.plusSeconds(17).atZone(ZONE).toInstant(), ZONE),
            new Home(List.of(Room.builder().name(RoomName.LIVING_ROOM).build(), Room.builder().name(RoomName.BEDROOM).build())),
            windowSensors,
            new SensorMonitorProperties("0 0 * * * *", Duration.ofHours(24), Duration.ofHours(24), Set.of()),
            alertRepository,
            notificationPublisher
        );
    }

    @Test
    void should_raise_an_alert_once_when_a_sensor_falls_silent() {
        when(windowSensors.silent(HEATED, NOW)).thenReturn(List.of(TERRACE_DOOR));
        when(alertRepository.findAll()).thenReturn(Flux.empty());
        when(notificationPublisher.publishAlert(SILENT_MESSAGE)).thenReturn(Mono.empty());
        when(alertRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        sut.check().as(StepVerifier::create).verifyComplete();

        verify(alertRepository).save(new OpeningSensorAlertEntity(null, 68L, LocalDateTime.of(2026, 10, 17, 20, 15), NOW));
    }

    @Test
    void should_say_when_a_sensor_never_reported() {
        when(windowSensors.silent(HEATED, NOW))
            .thenReturn(List.of(new WindowSensors.SilentSensor(68, "Terrace door", RoomName.LIVING_ROOM, null)));
        when(alertRepository.findAll()).thenReturn(Flux.empty());
        when(notificationPublisher.publishAlert(anyString())).thenReturn(Mono.empty());
        when(alertRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        sut.check().as(StepVerifier::create).verifyComplete();

        verify(notificationPublisher).publishAlert("Window sensor Terrace door in room living room is not reporting "
            + "(no state since the start of the service); the room heats as if it had no sensor");
        verify(alertRepository).save(new OpeningSensorAlertEntity(null, 68L, NOW, NOW));
    }

    //the row is written only once the message got through, so a refused message is repeated next time
    @Test
    void should_not_write_the_row_when_the_alert_did_not_get_through() {
        when(windowSensors.silent(HEATED, NOW)).thenReturn(List.of(TERRACE_DOOR));
        when(alertRepository.findAll()).thenReturn(Flux.empty());
        when(notificationPublisher.publishAlert(SILENT_MESSAGE)).thenReturn(Mono.error(new IllegalStateException("refused")));

        sut.check().as(StepVerifier::create).verifyComplete();

        verify(alertRepository, never()).save(any());
    }

    @Test
    void should_stay_quiet_within_the_reminder_interval() {
        when(windowSensors.silent(HEATED, NOW)).thenReturn(List.of(TERRACE_DOOR));
        when(alertRepository.findAll()).thenReturn(Flux.just(
            new OpeningSensorAlertEntity(7L, 68L, NOW.minusHours(23), NOW.minusHours(23))));

        sut.check().as(StepVerifier::create).verifyComplete();

        verify(notificationPublisher, never()).publishAlert(anyString());
        verify(notificationPublisher, never()).publishReminder(anyString());
        verify(alertRepository, never()).save(any());
    }

    @Test
    void should_remind_after_the_reminder_interval() {
        when(windowSensors.silent(HEATED, NOW)).thenReturn(List.of(TERRACE_DOOR));
        when(alertRepository.findAll()).thenReturn(Flux.just(
            new OpeningSensorAlertEntity(7L, 68L, NOW.minusHours(24), NOW.minusHours(24))));
        when(notificationPublisher.publishReminder(SILENT_MESSAGE)).thenReturn(Mono.empty());
        when(alertRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        sut.check().as(StepVerifier::create).verifyComplete();

        verify(alertRepository).save(new OpeningSensorAlertEntity(7L, 68L, NOW.minusHours(24), NOW));
    }

    @Test
    void should_announce_a_sensor_that_is_back() {
        when(windowSensors.silent(HEATED, NOW)).thenReturn(List.of());
        when(alertRepository.findAll()).thenReturn(Flux.just(
            new OpeningSensorAlertEntity(7L, 68L, NOW.minusHours(2), NOW.minusHours(2))));
        when(windowSensors.describe(68L)).thenReturn(Optional.of(
            new WindowSensors.Sensor(68, "Terrace door", RoomName.LIVING_ROOM, "38:39:8f:9c:ea:c3", true)));
        when(windowSensors.isKnown(68L, NOW)).thenReturn(true);
        when(notificationPublisher.publishInfo("Window sensor Terrace door in room living room is reporting again"))
            .thenReturn(Mono.empty());
        when(alertRepository.delete(any(OpeningSensorAlertEntity.class))).thenReturn(Mono.empty());

        sut.check().as(StepVerifier::create).verifyComplete();

        verify(alertRepository).delete(new OpeningSensorAlertEntity(7L, 68L, NOW.minusHours(2), NOW.minusHours(2)));
    }

    //not silent yet, not known either: the grace after a restart, or a gateway that has only just
    //stopped answering - no recovery, the row stays
    @Test
    void should_keep_the_row_of_a_sensor_not_known_again() {
        when(windowSensors.silent(HEATED, NOW)).thenReturn(List.of());
        when(alertRepository.findAll()).thenReturn(Flux.just(
            new OpeningSensorAlertEntity(7L, 68L, NOW.minusHours(2), NOW.minusHours(2))));
        when(windowSensors.describe(68L)).thenReturn(Optional.of(
            new WindowSensors.Sensor(68, "Terrace door", RoomName.LIVING_ROOM, "38:39:8f:9c:ea:c3", true)));
        when(windowSensors.isKnown(68L, NOW)).thenReturn(false);

        sut.check().as(StepVerifier::create).verifyComplete();

        verify(notificationPublisher, never()).publishInfo(anyString());
        verify(alertRepository, never()).delete(any(OpeningSensorAlertEntity.class));
    }

    //switched off, removed, or moved to a room without heating: no recovery to announce
    @Test
    void should_drop_the_row_of_a_sensor_no_longer_watched_without_a_message() {
        when(windowSensors.silent(HEATED, NOW)).thenReturn(List.of());
        when(alertRepository.findAll()).thenReturn(Flux.just(
            new OpeningSensorAlertEntity(7L, 68L, NOW.minusHours(2), NOW.minusHours(2)),
            new OpeningSensorAlertEntity(8L, 70L, NOW.minusHours(2), NOW.minusHours(2))));
        when(windowSensors.describe(68L)).thenReturn(Optional.empty());
        when(windowSensors.describe(70L)).thenReturn(Optional.of(
            new WindowSensors.Sensor(70, "Kitchen window", RoomName.KITCHEN, "7c:c6:b6:00:00:99", true)));
        when(alertRepository.delete(any(OpeningSensorAlertEntity.class))).thenReturn(Mono.empty());

        sut.check().as(StepVerifier::create).verifyComplete();

        verify(notificationPublisher, never()).publishInfo(anyString());
        verify(alertRepository).delete(new OpeningSensorAlertEntity(7L, 68L, NOW.minusHours(2), NOW.minusHours(2)));
        verify(alertRepository).delete(new OpeningSensorAlertEntity(8L, 70L, NOW.minusHours(2), NOW.minusHours(2)));
    }

    //Spring subscribes again to the same Mono for every run of a @Scheduled method: "now" is read at
    //every subscription, not when the Mono was built
    @Test
    void should_read_now_at_every_subscription() {
        final AtomicReference<LocalDateTime> now = new AtomicReference<>(NOW);
        final OpeningSensorMonitor monitor = new OpeningSensorMonitor(
            new Clock() {
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
                    return now.get().atZone(ZONE).toInstant();
                }
            },
            new Home(List.of(Room.builder().name(RoomName.LIVING_ROOM).build(), Room.builder().name(RoomName.BEDROOM).build())),
            windowSensors,
            new SensorMonitorProperties("0 0 * * * *", Duration.ofHours(24), Duration.ofHours(24), Set.of()),
            alertRepository,
            notificationPublisher
        );
        when(windowSensors.silent(any(), any())).thenReturn(List.of());
        when(alertRepository.findAll()).thenReturn(Flux.empty());

        final Mono<Void> check = monitor.check();
        check.as(StepVerifier::create).verifyComplete();
        now.set(NOW.plusMinutes(5));
        check.as(StepVerifier::create).verifyComplete();

        verify(windowSensors).silent(HEATED, NOW);
        verify(windowSensors).silent(HEATED, NOW.plusMinutes(5));
    }
}
