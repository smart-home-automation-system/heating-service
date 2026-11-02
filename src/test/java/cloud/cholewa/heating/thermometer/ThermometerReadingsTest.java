package cloud.cholewa.heating.thermometer;

import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Humidity;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.Temperature;
import cloud.cholewa.heating.registry.DeviceRegistryClient;
import cloud.cholewa.heating.service.TemperatureService;
import cloud.cholewa.home.model.DeviceKind;
import cloud.cholewa.home.model.EatonBinding;
import cloud.cholewa.home.model.EatonGatewayType;
import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.home.model.ShellyBinding;
import cloud.cholewa.home.model.SmartDevice;
import cloud.cholewa.home.model.TemperatureMessage;
import cloud.cholewa.shelly.model.ShellyComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cloud.cholewa.heating.thermometer.ThermometersTest.blu;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//what the BLU H&T of the bathrooms do to their rooms (HAS-364), on the real Thermometers and BluThermometers with only
//the registry and the storing of a reading faked: the gateways read as at a start of the service, then again five
//minutes later, and a reading published in between
class ThermometerReadingsTest {

    private static final ZoneId HOUSE = ZoneId.of("Europe/Warsaw");
    //2026-11-01 18:40 in the house
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-01T17:40:00Z"), HOUSE);
    private static final String PRESENCE_OFFICE = "shellypresence-ccba97ee868c";
    private static final String HALL_DOWN_DIMMER = "shellydimmerg3-0892724f4e30";
    private static final String BATH_UP = "38:39:8f:86:5f:bf";
    private static final String BATH_DOWN = "7c:c6:b6:2b:0d:e0";
    //17:38:20 UTC - 18:38:20 in the house
    private static final long HEARD = 1793554700L;

    private final DeviceRegistryClient registry = mock(DeviceRegistryClient.class);
    private final TemperatureService temperatureService = mock(TemperatureService.class);
    private final Thermometers thermometers = new Thermometers(registry);
    private final BluThermometers bluThermometers = new BluThermometers(thermometers, JsonMapper.builder().build());
    private final Room bathroomUp = room(RoomName.BATHROOM_UP);
    private final Room bathroomDown = room(RoomName.BATHROOM_DOWN);
    private final ThermometerReadings sut = new ThermometerReadings(thermometers, bluThermometers, temperatureService,
        new Home(List.of(bathroomUp, bathroomDown)), CLOCK);

    @BeforeEach
    void setUp() {
        when(temperatureService.handleTemperature(any())).thenReturn(Mono.empty());
    }

    private static Room room(final RoomName name) {
        return Room.builder().name(name).temperature(Temperature.builder().build()).humidity(new Humidity()).build();
    }

    private void registry(final SmartDevice... sensors) {
        when(registry.getTemperatureSensors()).thenReturn(Mono.just(List.of(sensors)));
        thermometers.refresh().as(StepVerifier::create).verifyComplete();
    }

    private static SmartDevice eaton(final long id, final RoomName room, final int point) {
        return new SmartDevice(id).room(room).kind(DeviceKind.TEMPERATURE_SENSOR).enabled(true)
            .eaton(new EatonBinding().gateway(EatonGatewayType.BLINDS).point(point));
    }

    //the three components of a BLU H&T as Shelly.GetComponents answers them, with the values it holds
    private static List<ShellyComponent> ht(final int first, final String address, final double temperature,
                                            final double humidity, final long heard) {
        return List.of(
            component(first, address, 0x01, 100, heard),
            component(first + 1, address, 0x2E, humidity, heard),
            component(first + 2, address, 0x45, temperature, heard));
    }

    private static ShellyComponent component(final int id, final String address, final int object, final Object value,
                                             final long heard) {
        final Map<String, Object> status = new HashMap<>();
        status.put("id", id);
        status.put("value", value);
        status.put("last_updated_ts", heard);
        return ShellyComponent.builder().key("bthomesensor:" + id)
            .config(new HashMap<>(Map.of("id", id, "addr", address, "obj_id", object, "idx", 0)))
            .status(status).build();
    }

    //DoD: the first value after a start comes from the read of the gateway - a BLU sensor publishes on a change
    //only - stored as a reading of a room without an Eaton sensor, dated when the gateway heard it; humidity answered
    @Test
    void should_take_the_first_reading_of_a_room_from_the_read_of_its_gateway() {
        registry(blu(137, RoomName.BATHROOM_DOWN, BATH_DOWN));

        sut.fromGateways(Map.of(HALL_DOWN_DIMMER, ht(204, BATH_DOWN, 26.9, 57, HEARD)))
            .as(StepVerifier::create).verifyComplete();

        verify(temperatureService).handleTemperature(new TemperatureMessage().room(RoomName.BATHROOM_DOWN)
            .temperature(26.9).date(LocalDateTime.of(2026, 11, 1, 18, 38, 20)));
        assertThat(bathroomDown.getHumidity().snapshot().getValue()).isEqualTo(57);
        assertThat(bathroomDown.getHumidity().snapshot().getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 11, 1, 18, 38, 20));
        assertThat(bluThermometers.target(HALL_DOWN_DIMMER, 206)).contains(
            new BluThermometers.Target(RoomName.BATHROOM_DOWN, BluThermometers.Metric.TEMPERATURE, BATH_DOWN));
    }

    //the gateway read again holds the same reading: not stored twice; a packet heard later is
    @Test
    void should_store_a_reading_of_a_gateway_once() {
        registry(blu(137, RoomName.BATHROOM_DOWN, BATH_DOWN));

        sut.fromGateways(Map.of(HALL_DOWN_DIMMER, ht(204, BATH_DOWN, 26.9, 57, HEARD))).as(StepVerifier::create).verifyComplete();
        sut.fromGateways(Map.of(HALL_DOWN_DIMMER, ht(204, BATH_DOWN, 26.9, 57, HEARD))).as(StepVerifier::create).verifyComplete();
        sut.fromGateways(Map.of(HALL_DOWN_DIMMER, ht(204, BATH_DOWN, 26.9, 57, HEARD + 60))).as(StepVerifier::create).verifyComplete();

        verify(temperatureService, times(2)).handleTemperature(any());
    }

    //a value a gateway still holds from more than half an hour ago is no reading of the room now (a dead battery)
    @Test
    void should_not_take_a_reading_the_gateway_heard_long_ago() {
        registry(blu(137, RoomName.BATHROOM_DOWN, BATH_DOWN));

        sut.fromGateways(Map.of(HALL_DOWN_DIMMER, ht(204, BATH_DOWN, 26.9, 57, HEARD - 3 * 86400)))
            .as(StepVerifier::create).verifyComplete();

        verify(temperatureService, never()).handleTemperature(any());
    }

    //after a rollout the room holds its last stored reading (seeded): the gateway holding the same one stores nothing
    @Test
    void should_not_store_again_the_reading_the_room_was_seeded_with() {
        registry(blu(137, RoomName.BATHROOM_DOWN, BATH_DOWN));
        bathroomDown.getTemperature().updateIfAbsent(26.9, LocalDateTime.of(2026, 11, 1, 18, 38, 20));

        sut.fromGateways(Map.of(HALL_DOWN_DIMMER, ht(204, BATH_DOWN, 26.9, 57, HEARD)))
            .as(StepVerifier::create).verifyComplete();

        verify(temperatureService, never()).handleTemperature(any());
    }

    //DoD: a room with an Eaton sensor ignores a BLU thermometer - mapped, neither stored nor its humidity taken
    @Test
    void should_ignore_a_blu_thermometer_of_a_room_with_an_eaton_sensor() {
        registry(eaton(8, RoomName.BATHROOM_UP, 52), blu(136, RoomName.BATHROOM_UP, BATH_UP));

        sut.fromGateways(Map.of(PRESENCE_OFFICE, ht(203, BATH_UP, 23.8, 64, HEARD)))
            .as(StepVerifier::create).verifyComplete();

        verify(temperatureService, never()).handleTemperature(any());
        assertThat(bathroomUp.getHumidity().snapshot().getUpdatedAt()).isNull();
        assertThat(bluThermometers.target(PRESENCE_OFFICE, 205)).isPresent();
    }

    //two thermometers in one room: the newer reading counts, an older one of the other is not taken
    @Test
    void should_take_the_newer_reading_of_two_thermometers_in_one_room() {
        registry(blu(137, RoomName.BATHROOM_DOWN, BATH_DOWN), new SmartDevice(150L).room(RoomName.BATHROOM_DOWN)
            .kind(DeviceKind.TEMPERATURE_SENSOR).enabled(true)
            .shelly(new ShellyBinding().address("192.0.2.50").generation(2).deviceId("shellyplusht-000000000150")));

        //the Wi-Fi one now (18:40), then the gateway with what it heard at 18:38:20 - older, not taken
        sut.temperature(RoomName.BATHROOM_DOWN, 25.0, null).as(StepVerifier::create).verifyComplete();
        sut.fromGateways(Map.of(HALL_DOWN_DIMMER, ht(204, BATH_DOWN, 26.9, 57, HEARD))).as(StepVerifier::create).verifyComplete();

        verify(temperatureService).handleTemperature(new TemperatureMessage().room(RoomName.BATHROOM_DOWN)
            .temperature(25.0).date(LocalDateTime.of(2026, 11, 1, 18, 40)));
        verify(temperatureService, times(1)).handleTemperature(any());
    }

    //a gateway ahead of the clock here dates its reading now; a reading that fails to store ends nothing
    @Test
    void should_date_a_reading_heard_ahead_of_the_clock_now_and_go_on_after_a_failure() {
        registry(blu(136, RoomName.BATHROOM_UP, BATH_UP), blu(137, RoomName.BATHROOM_DOWN, BATH_DOWN));
        when(temperatureService.handleTemperature(any())).thenReturn(Mono.error(new IllegalStateException("database")),
            Mono.empty());

        sut.fromGateways(Map.of(PRESENCE_OFFICE, ht(203, BATH_UP, 23.8, 64, HEARD),
                HALL_DOWN_DIMMER, ht(204, BATH_DOWN, 26.9, 57, HEARD + 3600)))
            .as(StepVerifier::create).verifyComplete();

        verify(temperatureService, times(2)).handleTemperature(any());
        verify(temperatureService).handleTemperature(new TemperatureMessage().room(RoomName.BATHROOM_DOWN)
            .temperature(26.9).date(LocalDateTime.of(2026, 11, 1, 18, 40)));
    }
}
