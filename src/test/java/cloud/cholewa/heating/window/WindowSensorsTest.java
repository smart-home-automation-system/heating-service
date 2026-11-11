package cloud.cholewa.heating.window;

import cloud.cholewa.heating.config.WindowProperties;
import cloud.cholewa.heating.registry.DeviceRegistryClient;
import cloud.cholewa.heating.registry.RegistryCallException;
import cloud.cholewa.home.model.BthomeBinding;
import cloud.cholewa.home.model.DeviceKind;
import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.home.model.ShellyBinding;
import cloud.cholewa.home.model.SmartDevice;
import cloud.cholewa.home.model.SmartVendor;
import cloud.cholewa.shelly.http.ShellyRpcClient;
import cloud.cholewa.shelly.model.BthomeSensorStatus;
import cloud.cholewa.shelly.model.ShellyComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//The windows and doors of a room from the opening sensors the registry places in it (HAS-232): the
//components of the gateways, their status topics, the hold after three minutes, and what a silent
//gateway does to its sensors
@ExtendWith(OutputCaptureExtension.class)
class WindowSensorsTest {

    private static final ZoneId HOUSE = ZoneId.of("Europe/Warsaw");
    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 17, 20, 0);
    //the time of the gateway the same as START, in epoch seconds
    private static final long START_TS = START.atZone(HOUSE).toEpochSecond();
    private static final String TREADMILL = "shellyplugsg3-28372f2f0460";
    private static final String OFFICE_MAIN = "shelly1pmg3-aabbccddeeff";
    private static final String WINDOW = "60:ef:ab:44:2b:dd";
    private static final String DOOR = "38:39:8f:9c:ea:c3";
    private static final String BEDROOM_WINDOW = "8c:6f:b9:13:f4:dd";
    private static final Set<RoomName> HEATED = Set.of(RoomName.LIVING_ROOM, RoomName.BEDROOM, RoomName.OFFICE);

    private final AtomicReference<LocalDateTime> now = new AtomicReference<>(START);
    private final DeviceRegistryClient registry = mock(DeviceRegistryClient.class);
    private final ShellyRpcClient rpc = mock(ShellyRpcClient.class);
    private final WindowSensorStore store = mock(WindowSensorStore.class);
    private final WindowSensors sut = new WindowSensors(registry, rpc, JsonMapper.builder().build(),
        new WindowProperties(Duration.ofMinutes(3), Duration.ofMinutes(30), Duration.ofHours(6)), store, new Clock() {
        @Override
        public ZoneId getZone() {
            return HOUSE;
        }

        @Override
        public Clock withZone(final ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get().atZone(HOUSE).toInstant();
        }
    });

    //the living room has a window and a terrace door; the bedroom a window; the office none
    private final List<SmartDevice> sensors = new ArrayList<>(List.of(
        sensor(67, "Living room window", RoomName.LIVING_ROOM, WINDOW, true),
        sensor(68, "Terrace door", RoomName.LIVING_ROOM, DOOR, true),
        sensor(56, "Bedroom window", RoomName.BEDROOM, BEDROOM_WINDOW, true)));

    @BeforeEach
    void setUp() {
        when(registry.getOpeningSensors()).thenAnswer(call -> Mono.just(List.copyOf(sensors)));
        when(registry.getBthomeGateways()).thenReturn(Mono.just(List.of(
            gateway(54, "TREADMILL", "10.0.0.36", TREADMILL),
            gateway(80, "OFFICE MAIN", "10.0.0.40", OFFICE_MAIN))));
        when(rpc.dynamicComponents("10.0.0.40")).thenReturn(Mono.just(List.of()));
        when(store.save(anyLong(), any())).thenReturn(Mono.empty());
        when(store.retain(any())).thenReturn(Mono.empty());
        when(store.load()).thenReturn(Flux.empty());
    }

    //HAS-419, the case of 2026-11-10: stored closed two seconds after an open; after a restart the gateway brings the
    //open back with its old time - the stored closed stays, now confirmed by the gateway, and the INFO tells it once
    @Test
    void should_keep_a_stored_reading_against_an_older_one_the_gateway_restored(final CapturedOutput output) {
        stored(67, false, START_TS + 2, null);
        gateway("10.0.0.36", window(202, WINDOW, true, START_TS));
        now.set(START.plusMinutes(10));

        refresh();
        refresh();

        assertThat(sut.valueOf(67, START.plusMinutes(10))).contains(new WindowSensors.WindowValue(false, START.plusSeconds(2)));
        assertThat(sut.windowOf(RoomName.LIVING_ROOM).state()).isNotEqualTo(WindowState.OPEN);
        assertThat(output.getOut()).containsOnlyOnce(
            "Window sensor 67 (Living room window): gateway " + TREADMILL + " gave open heard at " + START
                + ", older than the kept closed heard at " + START.plusSeconds(2) + " - the kept one stays");
        verify(store, never()).save(anyLong(), any());
    }

    //a newer value of the gateway replaces the stored one, and is stored
    @Test
    void should_replace_a_stored_reading_by_a_newer_one_of_the_gateway() {
        stored(67, false, START_TS, null);
        gateway("10.0.0.36", window(202, WINDOW, true, START_TS + 60));
        now.set(START.plusMinutes(5));

        refresh();

        assertThat(sut.valueOf(67, START.plusMinutes(5))).contains(new WindowSensors.WindowValue(true, START.plusMinutes(1)));
        verify(store).save(eq(67L), argThat(reading -> Boolean.TRUE.equals(reading.open())
            && Instant.ofEpochSecond(START_TS + 60).equals(reading.updatedAt()) && START.plusMinutes(1).equals(reading.openSince())));
    }

    //a stored reading no gateway stands for is never known - it neither holds nor frees a room - and the sensor is
    //silent once the gateways had their time after the start, as any sensor never heard of
    @Test
    void should_never_know_a_stored_reading_no_gateway_stands_for() {
        stored(67, true, START_TS - 3600, START.minusHours(1));
        gateway("10.0.0.36");
        refresh();

        assertThat(sut.valueOf(67, START)).isEmpty();
        assertThat(sut.windowOf(RoomName.LIVING_ROOM).state()).isEqualTo(WindowState.UNKNOWN);
        assertThat(sut.silent(HEATED, START.plusMinutes(30))).isEmpty();
        assertThat(sut.silent(HEATED, START.plusMinutes(31))).extracting(WindowSensors.SilentSensor::id).contains(67L);
    }

    //a component of the gateway without a window value, or a value that is no truth, confirms nothing read back from
    //the table: only a value of the gateway does
    @Test
    void should_not_confirm_a_stored_reading_by_a_component_without_a_value() {
        stored(56, true, START_TS - 600, START.minusMinutes(10));
        gateway("10.0.0.36", window(215, BEDROOM_WINDOW, null, null));
        refresh();
        assertThat(sut.valueOf(56, START)).isEmpty();

        sut.observe(TREADMILL, 215, status(215, 1, START_TS)).as(StepVerifier::create).verifyComplete();
        assertThat(sut.valueOf(56, START)).isEmpty();
        assertThat(sut.windowOf(RoomName.BEDROOM).state()).isEqualTo(WindowState.UNKNOWN);
    }

    //a confirmation is no change: a read that repeats a value stores nothing; the same value heard later is stored
    @Test
    void should_store_a_change_of_the_reading_only() {
        gateway("10.0.0.36", window(202, WINDOW, false, START_TS));
        refresh();
        refresh();
        verify(store, times(1)).save(anyLong(), any());

        sut.observe(TREADMILL, 202, status(202, false, START_TS + 600)).as(StepVerifier::create).verifyComplete();
        verify(store, times(2)).save(anyLong(), any());
        sut.observe(TREADMILL, 202, status(202, false, START_TS + 600)).as(StepVerifier::create).verifyComplete();
        verify(store, times(2)).save(anyLong(), any());
    }

    //the rows of the sensors the registry lists are kept, the others deleted - once the registry lists others, never
    //on a registry that lists none (one empty answer must not wipe what a restart compares with)
    @Test
    void should_keep_the_stored_readings_of_the_listed_sensors_only() {
        gateway("10.0.0.36");
        refresh();
        refresh();
        verify(store, times(1)).retain(Set.of(67L, 68L, 56L));

        sensors.remove(2);
        refresh();
        verify(store).retain(Set.of(67L, 68L));

        sensors.clear();
        refresh();
        verify(store, times(2)).retain(any());
    }

    //HAS-364: what every gateway answered is handed to the BLU thermometers; a gateway that fails keeps its last answer,
    //one gone from the registry leaves none
    @Test
    void should_keep_what_every_gateway_answered_for_the_thermometers() {
        final ShellyComponent window = window(202, WINDOW, false, START_TS);
        gateway("10.0.0.36", window);
        refresh();
        assertThat(sut.gatewayComponents()).containsOnlyKeys(TREADMILL.toLowerCase(), OFFICE_MAIN.toLowerCase());
        assertThat(sut.gatewayComponents().get(TREADMILL.toLowerCase())).containsExactly(window);

        when(rpc.dynamicComponents("10.0.0.36")).thenReturn(Mono.error(new IllegalStateException("timeout")));
        refresh();
        assertThat(sut.gatewayComponents().get(TREADMILL.toLowerCase())).containsExactly(window);

        when(registry.getBthomeGateways()).thenReturn(Mono.just(List.of(gateway(80, "OFFICE MAIN", "10.0.0.40", OFFICE_MAIN))));
        refresh();
        assertThat(sut.gatewayComponents()).containsOnlyKeys(OFFICE_MAIN.toLowerCase());
    }

    //a room without a sensor heats as it always did
    @Test
    void should_never_hold_a_room_without_a_sensor() {
        gateway("10.0.0.36", window(202, WINDOW, true, START_TS));
        refresh();

        assertThat(sut.windowOf(RoomName.OFFICE)).isEqualTo(RoomWindow.NO_SENSOR);
    }

    @Test
    void should_close_a_room_only_when_every_window_and_door_is_closed() {
        gateway("10.0.0.36", window(202, WINDOW, false, START_TS), window(206, DOOR, false, START_TS));
        refresh();
        assertThat(sut.windowOf(RoomName.LIVING_ROOM)).isEqualTo(new RoomWindow(WindowState.CLOSED, null, false));

        sut.observe(TREADMILL.toUpperCase(), 206, status(206, true, START_TS));
        assertThat(sut.windowOf(RoomName.LIVING_ROOM)).isEqualTo(new RoomWindow(WindowState.OPEN, START, false));
    }

    //owner, 2026-10-17: three minutes - a short trip through the terrace door does not stop the heating
    @Test
    void should_hold_the_heating_once_a_window_has_stood_open_for_three_minutes() {
        gateway("10.0.0.36", window(202, WINDOW, false, START_TS), window(206, DOOR, false, START_TS));
        refresh();
        sut.observe(TREADMILL, 206, status(206, true, START_TS));

        now.set(START.plusMinutes(2).plusSeconds(59));
        assertThat(sut.windowOf(RoomName.LIVING_ROOM).hold()).isFalse();

        now.set(START.plusMinutes(3));
        assertThat(sut.windowOf(RoomName.LIVING_ROOM)).isEqualTo(new RoomWindow(WindowState.OPEN, START, true));

        //closed again: the hold ends at once
        sut.observe(TREADMILL, 206, status(206, false, START_TS + 200));
        assertThat(sut.windowOf(RoomName.LIVING_ROOM)).isEqualTo(new RoomWindow(WindowState.CLOSED, null, false));
    }

    //the window that has stood open longest decides, and staying open does not move its start
    @Test
    void should_hold_by_the_window_open_longest() {
        gateway("10.0.0.36", window(202, WINDOW, true, START_TS), window(206, DOOR, false, START_TS));
        refresh();

        now.set(START.plusMinutes(2));
        sut.observe(TREADMILL, 206, status(206, true, START_TS + 120));
        sut.observe(TREADMILL, 202, status(202, true, START_TS + 125));

        now.set(START.plusMinutes(3));
        assertThat(sut.windowOf(RoomName.LIVING_ROOM)).isEqualTo(new RoomWindow(WindowState.OPEN, START, true));
    }

    //the gateway heard it open before this service did - after a restart, or between two reads
    @Test
    void should_count_the_open_time_from_when_the_gateway_heard_it() {
        gateway("10.0.0.36", window(56, BEDROOM_WINDOW, true, START_TS - 600));
        refresh();

        assertThat(sut.windowOf(RoomName.BEDROOM))
            .isEqualTo(new RoomWindow(WindowState.OPEN, START.minusMinutes(10), true));
    }

    //a clock of a gateway that runs ahead must not push the hold into the future
    @Test
    void should_not_take_an_open_time_ahead_of_the_clock() {
        gateway("10.0.0.36", window(56, BEDROOM_WINDOW, true, START_TS + 3600));
        refresh();

        assertThat(sut.windowOf(RoomName.BEDROOM).openSince()).isEqualTo(START);
    }

    //an open window is open even while the other sensor of the room is not known; an unknown one alone
    //never holds - a silent sensor must not leave a room cold
    @Test
    void should_hold_by_an_open_window_and_never_by_an_unknown_one() {
        gateway("10.0.0.36", window(202, WINDOW, true, START_TS - 600), window(206, DOOR, null, null));
        refresh();
        assertThat(sut.windowOf(RoomName.LIVING_ROOM).hold()).isTrue();

        sut.observe(TREADMILL, 202, status(202, false, START_TS + 1));
        assertThat(sut.windowOf(RoomName.LIVING_ROOM)).isEqualTo(new RoomWindow(WindowState.UNKNOWN, null, false));
    }

    //a sensor sends only on a change: a quiet window stays as it was as long as its gateway answers
    @Test
    void should_keep_a_state_while_the_gateway_confirms_it_and_lose_it_when_the_gateway_goes_silent() {
        gateway("10.0.0.36", window(56, BEDROOM_WINDOW, true, START_TS));
        refresh();

        now.set(START.plusHours(6));
        refresh();
        assertThat(sut.windowOf(RoomName.BEDROOM).hold()).isTrue();

        when(rpc.dynamicComponents("10.0.0.36")).thenReturn(Mono.error(new IllegalStateException("no route to host")));
        now.set(START.plusHours(6).plusMinutes(31));
        refresh();
        assertThat(sut.windowOf(RoomName.BEDROOM)).isEqualTo(new RoomWindow(WindowState.UNKNOWN, null, false));

        //a publish of the gateway stands for it again, and the window was open all along
        sut.observe(TREADMILL, 56, status(56, true, START_TS));
        assertThat(sut.windowOf(RoomName.BEDROOM)).isEqualTo(new RoomWindow(WindowState.OPEN, START, true));
    }

    //only the window object (0x2D) of a registered sensor
    @Test
    void should_map_only_the_window_of_a_registered_sensor() {
        final ShellyComponent battery = component("bthomesensor:200",
            Map.of("id", 200, "addr", BEDROOM_WINDOW, "obj_id", 1, "idx", 0),
            Map.of("id", 200, "value", 78, "last_updated_ts", START_TS));
        gateway("10.0.0.36", battery, window(219, "7c:c6:b6:00:00:99", true, START_TS), window(215, BEDROOM_WINDOW, false, START_TS));

        refresh();

        assertThat(sut.knows(TREADMILL, 215)).isTrue();
        assertThat(sut.knows(TREADMILL, 200)).isFalse();
        assertThat(sut.knows(TREADMILL, 219)).isFalse();
        assertThat(sut.knows(OFFICE_MAIN, 215)).isFalse();
        assertThat(sut.knows(null, 215)).isFalse();
        assertThat(sut.windowOf(RoomName.BEDROOM).state()).isEqualTo(WindowState.CLOSED);
    }

    //a read of the gateway may bring an older state than the publish that came a moment before
    @Test
    void should_keep_the_newer_of_two_reports() {
        gateway("10.0.0.36", window(215, BEDROOM_WINDOW, false, START_TS));
        refresh();
        sut.observe(TREADMILL, 215, status(215, true, START_TS + 200));

        refresh();

        assertThat(sut.windowOf(RoomName.BEDROOM).state()).isEqualTo(WindowState.OPEN);
    }

    //a switched-off sensor is left out of its room; with none left the room has no sensor
    @Test
    void should_leave_a_switched_off_sensor_out_of_its_room() {
        sensors.set(2, sensor(56, "Bedroom window", RoomName.BEDROOM, BEDROOM_WINDOW, false));
        gateway("10.0.0.36", window(215, BEDROOM_WINDOW, true, START_TS - 600));
        refresh();

        assertThat(sut.windowOf(RoomName.BEDROOM)).isEqualTo(RoomWindow.NO_SENSOR);
        assertThat(sut.silent(HEATED, START.plusHours(1))).extracting(WindowSensors.SilentSensor::id).doesNotContain(56L);
    }

    //a registry away keeps the sensors last read, and a refresh never fails
    @Test
    void should_keep_the_last_sensors_while_the_registry_is_away() {
        gateway("10.0.0.36", window(215, BEDROOM_WINDOW, false, START_TS));
        refresh();

        when(registry.getOpeningSensors()).thenReturn(Mono.error(new RegistryCallException("database-service answered 503")));
        when(registry.getBthomeGateways()).thenReturn(Mono.error(new RegistryCallException("database-service answered 503")));
        for (int minutes = 5; minutes <= 60; minutes += 5) {
            now.set(START.plusMinutes(minutes));
            refresh();
        }

        assertThat(sut.windowOf(RoomName.BEDROOM).state()).isEqualTo(WindowState.CLOSED);
    }

    //silent: no known state for gateway-silent-after - since the registry first listed it, or since
    //its gateway last stood for a state; only switched-on sensors of the rooms asked about
    @Test
    void should_report_a_sensor_silent_after_the_gateways_had_their_time() {
        gateway("10.0.0.36", window(202, WINDOW, false, START_TS), window(56, BEDROOM_WINDOW, false, START_TS));
        refresh();

        //the terrace door was never heard of: silent once half an hour has passed since the registry
        //listed it, while the gateway keeps standing for the other two
        assertThat(sut.silent(HEATED, START.plusMinutes(30))).isEmpty();
        now.set(START.plusMinutes(31));
        refresh();
        assertThat(sut.silent(HEATED, START.plusMinutes(31)))
            .containsExactly(new WindowSensors.SilentSensor(68, "Terrace door", RoomName.LIVING_ROOM, null));
        assertThat(sut.silent(Set.of(RoomName.BEDROOM), START.plusMinutes(31))).isEmpty();

        //the gateway of the other two goes away: silent half an hour after it last stood for them
        when(rpc.dynamicComponents("10.0.0.36")).thenReturn(Mono.error(new IllegalStateException("no route to host")));
        now.set(START.plusMinutes(36));
        refresh();
        assertThat(sut.silent(HEATED, START.plusMinutes(61))).extracting(WindowSensors.SilentSensor::id).containsExactly(68L);
        assertThat(sut.silent(HEATED, START.plusMinutes(62)))
            .containsExactly(
                new WindowSensors.SilentSensor(56, "Bedroom window", RoomName.BEDROOM, START.plusMinutes(31)),
                new WindowSensors.SilentSensor(67, "Living room window", RoomName.LIVING_ROOM, START.plusMinutes(31)),
                new WindowSensors.SilentSensor(68, "Terrace door", RoomName.LIVING_ROOM, null));
    }

    //a sensor switched on in the registry later has its half hour from then, not from its registration
    @Test
    void should_give_a_sensor_switched_on_later_its_time() {
        sensors.set(1, sensor(68, "Terrace door", RoomName.LIVING_ROOM, DOOR, false));
        gateway("10.0.0.36", window(202, WINDOW, false, START_TS), window(56, BEDROOM_WINDOW, false, START_TS));
        refresh();

        now.set(START.plusHours(2));
        sensors.set(1, sensor(68, "Terrace door", RoomName.LIVING_ROOM, DOOR, true));
        refresh();

        assertThat(sut.silent(HEATED, START.plusHours(2).plusMinutes(30))).isEmpty();
        assertThat(sut.silent(HEATED, START.plusHours(2).plusMinutes(31)))
            .extracting(WindowSensors.SilentSensor::id).contains(68L);
    }

    //a value that is no truth is no window, but the gateway stood for the sensor
    @Test
    void should_ignore_a_value_that_is_no_truth() {
        gateway("10.0.0.36", window(215, BEDROOM_WINDOW, false, START_TS));
        refresh();

        sut.observe(TREADMILL, 215, status(215, 1, START_TS + 300));

        assertThat(sut.windowOf(RoomName.BEDROOM).state()).isEqualTo(WindowState.CLOSED);
    }

    //HAS-350: the age of an open window is the time its gateway last heard the window value - a battery packet of the
    //same sensor (another component, a newer time) and a read repeating the same value leave it where it was
    @Test
    void should_age_an_open_window_by_its_window_value_only() {
        final ShellyComponent battery = component("bthomesensor:207",
            Map.of("id", 207, "addr", DOOR, "obj_id", 1, "idx", 0),
            Map.of("id", 207, "value", 100, "last_updated_ts", START_TS + 7200));
        gateway("10.0.0.36", window(206, DOOR, true, START_TS), battery);
        refresh();

        now.set(START.plusHours(3));
        refresh();

        assertThat(sut.open(HEATED, START.plusHours(3)))
            .containsExactly(new WindowSensors.OpenSensor(68, "Terrace door", RoomName.LIVING_ROOM, START, START));
        assertThat(sut.valueOf(68, START.plusHours(3))).contains(new WindowSensors.WindowValue(true, START));
    }

    //a newer window value moves it: closed, or an open heard later
    @Test
    void should_give_the_time_of_a_newer_window_value() {
        gateway("10.0.0.36", window(206, DOOR, true, START_TS));
        refresh();

        sut.observe(TREADMILL, 206, status(206, false, START_TS + 600));

        assertThat(sut.valueOf(68, START.plusMinutes(11))).contains(new WindowSensors.WindowValue(false, START.plusMinutes(10)));
        assertThat(sut.open(HEATED, START.plusMinutes(11))).isEmpty();
    }

    //a sensor its gateway no longer stands for is neither open nor closed
    @Test
    void should_have_no_value_of_a_sensor_not_known() {
        gateway("10.0.0.36", window(206, DOOR, true, START_TS));
        refresh();

        final LocalDateTime later = START.plusMinutes(31);
        assertThat(sut.valueOf(68, later)).isEmpty();
        assertThat(sut.open(HEATED, later)).isEmpty();
    }

    private void refresh() {
        sut.refresh().as(StepVerifier::create).verifyComplete();
    }

    //a reading stored before a restart, read back at the start
    private void stored(final long sensor, final boolean open, final long updatedAt, final LocalDateTime openSince) {
        when(store.load()).thenReturn(Flux.just(new WindowSensorStore.Stored(sensor,
            new WindowSensors.Reading(open, Instant.ofEpochSecond(updatedAt), null, openSince))));
        sut.restore().as(StepVerifier::create).verifyComplete();
    }

    private void gateway(final String address, final ShellyComponent... components) {
        when(rpc.dynamicComponents(address)).thenReturn(Mono.just(List.of(components)));
    }

    private static ShellyComponent window(final int id, final String address, final Boolean open, final Long updatedAt) {
        final Map<String, Object> status = new HashMap<>();
        status.put("id", id);
        if (open != null) {
            status.put("value", open);
            status.put("last_updated_ts", updatedAt);
        }
        return component("bthomesensor:" + id, Map.of("id", id, "addr", address, "obj_id", 45, "idx", 0), status);
    }

    private static ShellyComponent component(final String key, final Map<String, Object> config, final Map<String, Object> status) {
        return ShellyComponent.builder().key(key).config(new HashMap<>(config)).status(new HashMap<>(status)).build();
    }

    private static BthomeSensorStatus status(final int id, final Object value, final long updatedAt) {
        return BthomeSensorStatus.builder().id(id).value(value).lastUpdatedTs(updatedAt).build();
    }

    private static SmartDevice sensor(final long id, final String name, final RoomName room, final String address, final boolean enabled) {
        return SmartDevice.builder().id(id).name(name).room(room).kind(DeviceKind.OPENING_SENSOR)
            .vendor(SmartVendor.SHELLY).enabled(enabled).bthome(BthomeBinding.builder().address(address).build()).build();
    }

    private static SmartDevice gateway(final long id, final String name, final String address, final String deviceId) {
        return SmartDevice.builder().id(id).name(name).room(RoomName.BEDROOM).kind(DeviceKind.BTHOME_GATEWAY)
            .vendor(SmartVendor.SHELLY).enabled(true)
            .shelly(ShellyBinding.builder().address(address).generation(3).deviceId(deviceId).build()).build();
    }
}
