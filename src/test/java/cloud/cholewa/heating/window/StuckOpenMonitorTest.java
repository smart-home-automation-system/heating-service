package cloud.cholewa.heating.window;

import cloud.cholewa.commons.notification.NotificationPublisher;
import cloud.cholewa.heating.config.WindowProperties;
import cloud.cholewa.heating.db.model.OpeningSensorOpenAlertEntity;
import cloud.cholewa.heating.db.repository.OpeningSensorOpenAlertRepository;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.HomeStatus;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//A window sensor that may be stuck open (HAS-350): told once, and only while its hold costs heat - the heating of
//the house on, a heater of the room in schedule (below its target); ended with one info by a newer window value,
//without a message for a sensor no longer watched; the message first, the row second
@ExtendWith(MockitoExtension.class)
class StuckOpenMonitorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 30, 21, 0);
    private static final LocalDateTime OPENED = LocalDateTime.of(2026, 10, 25, 16, 52);
    private static final Set<RoomName> HEATED = Set.of(RoomName.OFFICE, RoomName.BEDROOM);
    private static final WindowSensors.OpenSensor OFFICE_DOOR =
        new WindowSensors.OpenSensor(61, "Office door", RoomName.OFFICE, OPENED, OPENED);
    private static final RoomWindow HELD = new RoomWindow(WindowState.OPEN, OPENED, true);
    private static final String STUCK_MESSAGE = "Window sensor Office door in room office has read open since "
        + "2026-10-25 16:52 without a change; the heating of the room is held. If it is closed, open and close it once "
        + "- a lost 'closed' packet";
    private static final OpeningSensorOpenAlertEntity TOLD =
        new OpeningSensorOpenAlertEntity(4L, 61L, OPENED, OPENED, NOW.minusDays(2));

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private WindowSensors windowSensors;
    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private OpeningSensorOpenAlertRepository alertRepository;
    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private NotificationPublisher notificationPublisher;

    private final HomeStatus homeStatus = new HomeStatus();
    private final HeaterActor officeRadiator = HeaterActor.builder().type(HeaterType.RADIATOR).build();
    private StuckOpenMonitor sut;

    @BeforeEach
    void setUp() {
        homeStatus.setEnabledHomeHeatingSystem(true);
        officeRadiator.setInSchedule(true);
        sut = monitor(Clock.fixed(NOW.plusSeconds(17).atZone(ZONE).toInstant(), ZONE));
    }

    private StuckOpenMonitor monitor(final Clock clock) {
        return new StuckOpenMonitor(
            clock,
            new Home(List.of(
                Room.builder().name(RoomName.OFFICE).heaterActor(officeRadiator).build(),
                Room.builder().name(RoomName.BEDROOM).build())),
            homeStatus,
            windowSensors,
            new WindowProperties(Duration.ofMinutes(3), Duration.ofMinutes(30), Duration.ofHours(6)),
            alertRepository,
            notificationPublisher);
    }

    private void open(final WindowSensors.OpenSensor... sensors) {
        when(windowSensors.open(HEATED, NOW)).thenReturn(List.of(sensors));
    }

    private void check() {
        sut.check().as(StepVerifier::create).verifyComplete();
    }

    private static WindowSensors.Sensor office(final long id, final String name) {
        return new WindowSensors.Sensor(id, name, RoomName.OFFICE, "60:ef:ab:44:2b:dd", true);
    }

    //the office door of 2026-10-30: read open for five days while the room called for heat
    @Test
    void should_tell_a_window_stuck_open_while_its_room_would_heat() {
        open(OFFICE_DOOR);
        when(windowSensors.windowOf(RoomName.OFFICE, NOW)).thenReturn(HELD);
        when(alertRepository.findAll()).thenReturn(Flux.empty());
        when(notificationPublisher.publishReminder(STUCK_MESSAGE)).thenReturn(Mono.empty());
        when(alertRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        check();

        verify(alertRepository).save(new OpeningSensorOpenAlertEntity(null, 61L, OPENED, OPENED, NOW));
    }

    //summer: the room is above its target, no heater is in schedule - a door left open says nothing
    @Test
    void should_say_nothing_while_the_room_would_not_heat() {
        officeRadiator.setInSchedule(false);
        open(OFFICE_DOOR);
        lenient().when(windowSensors.windowOf(RoomName.OFFICE, NOW)).thenReturn(HELD);
        when(alertRepository.findAll()).thenReturn(Flux.empty());

        check();

        verify(notificationPublisher, never()).publishReminder(anyString());
        verify(alertRepository, never()).save(any());
    }

    @Test
    void should_say_nothing_while_the_heating_of_the_house_is_off() {
        homeStatus.setEnabledHomeHeatingSystem(false);
        open(OFFICE_DOOR);
        lenient().when(windowSensors.windowOf(RoomName.OFFICE, NOW)).thenReturn(HELD);
        when(alertRepository.findAll()).thenReturn(Flux.empty());

        check();

        verify(notificationPublisher, never()).publishReminder(anyString());
    }

    //a window opened an hour ago is an open window, not a stuck one
    @Test
    void should_wait_six_hours_without_a_new_window_value() {
        open(new WindowSensors.OpenSensor(61, "Office door", RoomName.OFFICE, NOW.minusHours(6).plusMinutes(1),
            NOW.minusHours(6).plusMinutes(1)));
        when(alertRepository.findAll()).thenReturn(Flux.empty());

        check();

        verify(notificationPublisher, never()).publishReminder(anyString());
    }

    //the age is that of the window value: open for a week, heard again an hour ago is not stuck
    @Test
    void should_count_the_age_from_the_last_window_value() {
        open(new WindowSensors.OpenSensor(61, "Office door", RoomName.OFFICE, OPENED, NOW.minusHours(1)));
        when(alertRepository.findAll()).thenReturn(Flux.empty());

        check();

        verify(notificationPublisher, never()).publishReminder(anyString());
    }

    @Test
    void should_say_nothing_while_the_room_is_not_held() {
        open(OFFICE_DOOR);
        when(windowSensors.windowOf(RoomName.OFFICE, NOW)).thenReturn(new RoomWindow(WindowState.OPEN, OPENED, false));
        when(alertRepository.findAll()).thenReturn(Flux.empty());

        check();

        verify(notificationPublisher, never()).publishReminder(anyString());
    }

    //a restart, or the next pass: the row says it was told - no reminder, ever
    @Test
    void should_tell_a_stuck_window_once() {
        open(OFFICE_DOOR);
        when(alertRepository.findAll()).thenReturn(Flux.just(TOLD));
        when(windowSensors.describe(61)).thenReturn(Optional.of(office(61, "Office door")));
        when(windowSensors.valueOf(61, NOW)).thenReturn(Optional.of(new WindowSensors.WindowValue(true, OPENED)));

        check();

        verify(notificationPublisher, never()).publishReminder(anyString());
        verify(notificationPublisher, never()).publishInfo(anyString());
        verify(alertRepository, never()).delete(any(OpeningSensorOpenAlertEntity.class));
    }

    @Test
    void should_not_write_the_row_when_the_message_did_not_get_through() {
        open(OFFICE_DOOR);
        when(windowSensors.windowOf(RoomName.OFFICE, NOW)).thenReturn(HELD);
        when(alertRepository.findAll()).thenReturn(Flux.empty());
        when(notificationPublisher.publishReminder(STUCK_MESSAGE)).thenReturn(Mono.error(new IllegalStateException("nack")));

        check();

        verify(alertRepository, never()).save(any());
    }

    //opened and closed once: the gateway heard "closed" - one info, the row goes
    @Test
    void should_end_with_one_info_when_a_newer_value_comes() {
        when(windowSensors.open(HEATED, NOW)).thenReturn(List.of());
        when(alertRepository.findAll()).thenReturn(Flux.just(TOLD));
        when(windowSensors.describe(61)).thenReturn(Optional.of(office(61, "Office door")));
        when(windowSensors.valueOf(61, NOW)).thenReturn(Optional.of(new WindowSensors.WindowValue(false, NOW.minusMinutes(4))));
        when(notificationPublisher.publishInfo("Window sensor Office door in room office reports again: closed"))
            .thenReturn(Mono.empty());
        when(alertRepository.delete(TOLD)).thenReturn(Mono.empty());

        check();

        verify(alertRepository).delete(TOLD);
    }

    //an open heard after the one told is a new value too - the window moved and opened again
    @Test
    void should_end_with_a_newer_open_value() {
        final WindowSensors.OpenSensor reopened = new WindowSensors.OpenSensor(61, "Office door", RoomName.OFFICE,
            NOW.minusMinutes(5), NOW.minusMinutes(5));
        open(reopened);
        when(alertRepository.findAll()).thenReturn(Flux.just(TOLD));
        when(windowSensors.describe(61)).thenReturn(Optional.of(office(61, "Office door")));
        when(windowSensors.valueOf(61, NOW)).thenReturn(Optional.of(new WindowSensors.WindowValue(true, NOW.minusMinutes(5))));
        when(notificationPublisher.publishInfo("Window sensor Office door in room office reports again: open"))
            .thenReturn(Mono.empty());
        when(alertRepository.delete(TOLD)).thenReturn(Mono.empty());

        check();

        verify(alertRepository).delete(TOLD);
    }

    //its gateway silent: OpeningSensorMonitor tells that; the row stays
    @Test
    void should_keep_the_row_of_a_sensor_not_known() {
        when(windowSensors.open(HEATED, NOW)).thenReturn(List.of());
        when(alertRepository.findAll()).thenReturn(Flux.just(TOLD));
        when(windowSensors.describe(61)).thenReturn(Optional.of(office(61, "Office door")));
        when(windowSensors.valueOf(61, NOW)).thenReturn(Optional.empty());

        check();

        verify(alertRepository, never()).delete(any(OpeningSensorOpenAlertEntity.class));
    }

    //switched off, gone from the registry or moved to a room this service does not heat
    @Test
    void should_drop_the_row_of_a_sensor_no_longer_watched_without_a_message() {
        when(windowSensors.open(HEATED, NOW)).thenReturn(List.of());
        when(alertRepository.findAll()).thenReturn(Flux.just(TOLD));
        when(windowSensors.describe(61)).thenReturn(Optional.empty());
        when(alertRepository.delete(TOLD)).thenReturn(Mono.empty());

        check();

        verify(alertRepository).delete(TOLD);
        verify(notificationPublisher, never()).publishInfo(anyString());
    }

    @Test
    void should_complete_when_the_rows_cannot_be_read() {
        open(OFFICE_DOOR);
        when(alertRepository.findAll()).thenReturn(Flux.error(new IllegalStateException("database down")));

        check();

        verify(notificationPublisher, never()).publishReminder(anyString());
    }

    //Spring subscribes again to the same Mono for every run of a @Scheduled method: "now" is read at every
    //subscription - six hours later the same open window is stuck
    @Test
    void should_read_now_at_every_subscription() {
        final AtomicReference<LocalDateTime> now = new AtomicReference<>(OPENED.plusHours(1));
        final StuckOpenMonitor monitor = monitor(new Clock() {
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
        });
        when(windowSensors.open(any(), any())).thenReturn(List.of(OFFICE_DOOR));
        when(windowSensors.windowOf(any(), any())).thenReturn(HELD);
        when(alertRepository.findAll()).thenReturn(Flux.empty());
        when(notificationPublisher.publishReminder(STUCK_MESSAGE)).thenReturn(Mono.empty());
        when(alertRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        final Mono<Void> pass = monitor.check();

        pass.as(StepVerifier::create).verifyComplete();
        now.set(OPENED.plusHours(6));
        pass.as(StepVerifier::create).verifyComplete();

        verify(notificationPublisher).publishReminder(STUCK_MESSAGE);
    }
}
