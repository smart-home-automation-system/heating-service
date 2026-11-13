package cloud.cholewa.heating.thermometer;

import cloud.cholewa.heating.registry.DeviceRegistryClient;
import cloud.cholewa.heating.registry.RegistryCallException;
import cloud.cholewa.home.model.BthomeBinding;
import cloud.cholewa.home.model.DeviceKind;
import cloud.cholewa.home.model.EatonBinding;
import cloud.cholewa.home.model.EatonGatewayType;
import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.home.model.ShellyBinding;
import cloud.cholewa.home.model.SmartDevice;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

//which Shelly thermometers of the registry are read: the Wi-Fi ones by their deviceId (HAS-360) and the BLU ones by
//their address (HAS-364); a room with an Eaton room sensor keeps it, and of two thermometers the newer reading counts
class ThermometersTest {

    private static final String SANCTUM_HT = "shellyplusht-b0b21c138428";

    private final DeviceRegistryClient registry = mock(DeviceRegistryClient.class);
    private final Thermometers thermometers = new Thermometers(registry);

    private static final String BATH_UP = "38:39:8f:86:5f:bf";
    private static final String BATH_DOWN = "7c:c6:b6:2b:0d:e0";
    private static final LocalDateTime NOON = LocalDateTime.of(2026, 11, 1, 12, 0);

    static SmartDevice blu(final long id, final RoomName room, final String address) {
        return new SmartDevice(id).name("Thermometer").room(room).kind(DeviceKind.TEMPERATURE_SENSOR).enabled(true)
            .bthome(new BthomeBinding().address(address));
    }

    private static SmartDevice eaton(final long id, final RoomName room, final int point) {
        return new SmartDevice(id).name("Temperature sensor " + point).room(room).kind(DeviceKind.TEMPERATURE_SENSOR)
            .enabled(true).eaton(new EatonBinding().gateway(EatonGatewayType.BLINDS).point(point));
    }

    private static SmartDevice shelly(final long id, final RoomName room, final String deviceId) {
        return new SmartDevice(id).name("Temperature sensor").room(room).kind(DeviceKind.TEMPERATURE_SENSOR)
            .enabled(true).shelly(new ShellyBinding().address("192.0.2.32").generation(2).deviceId(deviceId));
    }

    @Test
    void should_read_the_shelly_thermometer_of_a_room_without_an_eaton_sensor() {
        when(registry.getTemperatureSensors()).thenReturn(Mono.just(List.of(eaton(10, RoomName.CINEMA, 54),
            shelly(104, RoomName.SANCTUM, SANCTUM_HT))));

        thermometers.refresh().as(StepVerifier::create).verifyComplete();

        assertThat(thermometers.roomOf(SANCTUM_HT)).contains(RoomName.SANCTUM);
        assertThat(thermometers.roomOf("shellyplusht-000000000000")).isEmpty();
        assertThat(thermometers.roomOf(null)).isEmpty();
    }

    //DoD: a room with an Eaton room sensor keeps the Eaton reading - its Shelly thermometer is not read
    @Test
    void should_never_read_a_shelly_thermometer_of_a_room_with_an_eaton_sensor() {
        assertThat(Thermometers.read(List.of(eaton(10, RoomName.CINEMA, 54),
            shelly(120, RoomName.CINEMA, "shellyplusht-c0c0c0c0c0c0"))).wifi()).isEmpty();
    }

    //amx-service publishes a switched-off Eaton sensor all the same: its room stays Eaton
    @Test
    void should_never_read_a_shelly_thermometer_of_a_room_with_a_switched_off_eaton_sensor() {
        assertThat(Thermometers.read(List.of(eaton(10, RoomName.CINEMA, 54).enabled(false),
            shelly(120, RoomName.CINEMA, "shellyplusht-c0c0c0c0c0c0"))).wifi()).isEmpty();
    }

    @Test
    void should_skip_a_switched_off_thermometer() {
        final Thermometers.Known read = Thermometers.read(List.of(shelly(104, RoomName.SANCTUM, SANCTUM_HT).enabled(false),
            blu(136, RoomName.BATHROOM_UP, BATH_UP).enabled(false)));

        assertThat(read.wifi()).isEmpty();
        assertThat(read.blu()).isEmpty();
    }

    //HAS-368: a Gen1 thermometer by its address - a Shelly binding of generation 1, which has no MQTT id
    @Test
    void should_read_a_gen1_thermometer_by_its_address() {
        final SmartDevice sauna = new SmartDevice(150L).name("Sauna temperature").room(RoomName.SAUNA)
            .kind(DeviceKind.TEMPERATURE_SENSOR).enabled(true)
            .shelly(new ShellyBinding().address("192.0.2.42").generation(1));
        final SmartDevice cinema = new SmartDevice(151L).name("Temperature").room(RoomName.CINEMA)
            .kind(DeviceKind.TEMPERATURE_SENSOR).enabled(true)
            .shelly(new ShellyBinding().address("192.0.2.43").generation(1));

        final Thermometers.Known read = Thermometers.read(List.of(sauna, cinema, eaton(10, RoomName.CINEMA, 54),
            shelly(104, RoomName.SANCTUM, SANCTUM_HT)));

        assertThat(read.gen1()).containsExactly(Map.entry("192.0.2.42", RoomName.SAUNA));
        assertThat(read.wifi()).containsOnlyKeys(SANCTUM_HT);
    }

    @Test
    void should_skip_a_switched_off_gen1_thermometer_or_one_without_an_address() {
        final SmartDevice off = new SmartDevice(150L).room(RoomName.SAUNA).kind(DeviceKind.TEMPERATURE_SENSOR)
            .enabled(false).shelly(new ShellyBinding().address("192.0.2.42").generation(1));
        final SmartDevice noAddress = new SmartDevice(152L).room(RoomName.GARDEN).kind(DeviceKind.TEMPERATURE_SENSOR)
            .enabled(true).shelly(new ShellyBinding().generation(1));

        assertThat(Thermometers.read(List.of(off, noAddress)).gen1()).isEmpty();
    }

    //HAS-364: a BLU H&T by its address in lower case, in any room - mapped in an Eaton room too, where accept drops it
    @Test
    void should_read_a_blu_thermometer_by_its_address() {
        when(registry.getTemperatureSensors()).thenReturn(Mono.just(List.of(eaton(8, RoomName.BATHROOM_UP, 52),
            blu(136, RoomName.BATHROOM_UP, BATH_UP.toUpperCase()), blu(137, RoomName.BATHROOM_DOWN, BATH_DOWN))));

        thermometers.refresh().as(StepVerifier::create).verifyComplete();

        assertThat(thermometers.bluRoomOf(BATH_UP)).contains(RoomName.BATHROOM_UP);
        assertThat(thermometers.bluRoomOf(BATH_DOWN.toUpperCase())).contains(RoomName.BATHROOM_DOWN);
        assertThat(thermometers.bluRoomOf("7c:c6:b6:00:00:00")).isEmpty();
        assertThat(thermometers.bluRoomOf(null)).isEmpty();
        assertThat(thermometers.roomOf(BATH_UP)).isEmpty();
    }

    //DoD: a room with an Eaton sensor ignores a BLU thermometer - its readings are never taken
    @Test
    void should_never_take_a_reading_into_a_room_with_an_eaton_sensor() {
        when(registry.getTemperatureSensors()).thenReturn(Mono.just(List.of(eaton(8, RoomName.BATHROOM_UP, 52),
            blu(136, RoomName.BATHROOM_UP, BATH_UP), blu(137, RoomName.BATHROOM_DOWN, BATH_DOWN))));
        thermometers.refresh().as(StepVerifier::create).verifyComplete();

        assertThat(thermometers.reads(RoomName.BATHROOM_UP)).isFalse();
        assertThat(thermometers.accept(RoomName.BATHROOM_UP, NOON, null)).isFalse();
        assertThat(thermometers.accept(RoomName.BATHROOM_DOWN, NOON, null)).isTrue();
    }

    //the same reading read again from its gateway, or an older one of a second thermometer, is not taken again
    @Test
    void should_take_only_a_reading_newer_than_the_last_one_of_the_room() {
        assertThat(thermometers.accept(RoomName.BATHROOM_DOWN, NOON, null)).isTrue();
        assertThat(thermometers.accept(RoomName.BATHROOM_DOWN, NOON, null)).isFalse();
        assertThat(thermometers.accept(RoomName.BATHROOM_DOWN, NOON.minusMinutes(1), null)).isFalse();
        assertThat(thermometers.accept(RoomName.BATHROOM_DOWN, NOON.plusMinutes(1), null)).isTrue();
        assertThat(thermometers.accept(RoomName.BATHROOM_UP, NOON, null)).isTrue();
        assertThat(thermometers.accept(RoomName.BATHROOM_UP, null, null)).isFalse();
        assertThat(thermometers.accept(null, NOON, null)).isFalse();
    }

    //HAS-364: two thermometers in one room are both read - the newer reading counts (accept), not the lower id
    @Test
    void should_read_both_thermometers_of_one_room() {
        assertThat(Thermometers.read(List.of(shelly(140, RoomName.SANCTUM, "shellyplusht-later"),
            shelly(104, RoomName.SANCTUM, SANCTUM_HT))).wifi()).containsOnlyKeys(SANCTUM_HT, "shellyplusht-later");
    }

    //after a start the reading the room holds (the last stored one, seeded) counts until a reading is taken: the gateway
    //still holding that same reading does not store it twice
    @Test
    void should_count_the_reading_the_room_holds_before_the_first_one_taken() {
        assertThat(thermometers.accept(RoomName.BATHROOM_DOWN, NOON, NOON)).isFalse();
        assertThat(thermometers.accept(RoomName.BATHROOM_DOWN, NOON.plusMinutes(1), NOON)).isTrue();
        assertThat(thermometers.accept(RoomName.BATHROOM_DOWN, NOON.plusMinutes(2), NOON.plusHours(1))).isTrue();
    }

    @Test
    void should_keep_the_last_thermometers_when_the_registry_cannot_be_read() {
        when(registry.getTemperatureSensors()).thenReturn(Mono.just(List.of(shelly(104, RoomName.SANCTUM, SANCTUM_HT))),
            Mono.error(new RegistryCallException("database-service answered 500")));

        thermometers.refresh().as(StepVerifier::create).verifyComplete();
        thermometers.refresh().as(StepVerifier::create).verifyComplete();

        assertThat(thermometers.roomOf(SANCTUM_HT)).contains(RoomName.SANCTUM);
    }
}
