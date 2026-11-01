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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

//which Shelly thermometers of the registry are read (HAS-360): the temperature sensors with a deviceId, in a room
//without an Eaton room sensor; a BLU H&T of a gateway is HAS-364's
class ThermometersTest {

    private static final String SANCTUM_HT = "shellyplusht-b0b21c138428";

    private final DeviceRegistryClient registry = mock(DeviceRegistryClient.class);
    private final Thermometers thermometers = new Thermometers(registry);

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
            shelly(120, RoomName.CINEMA, "shellyplusht-c0c0c0c0c0c0")))).isEmpty();
    }

    //amx-service publishes a switched-off Eaton sensor all the same: its room stays Eaton
    @Test
    void should_never_read_a_shelly_thermometer_of_a_room_with_a_switched_off_eaton_sensor() {
        assertThat(Thermometers.read(List.of(eaton(10, RoomName.CINEMA, 54).enabled(false),
            shelly(120, RoomName.CINEMA, "shellyplusht-c0c0c0c0c0c0")))).isEmpty();
    }

    @Test
    void should_skip_a_switched_off_thermometer_and_a_bluetooth_one() {
        final SmartDevice off = shelly(104, RoomName.SANCTUM, SANCTUM_HT).enabled(false);
        final SmartDevice blu = new SmartDevice(130L).room(RoomName.BATHROOM_UP).kind(DeviceKind.TEMPERATURE_SENSOR)
            .enabled(true).bthome(new BthomeBinding().address("7c:c6:b6:00:00:01"));

        assertThat(Thermometers.read(List.of(off, blu))).isEmpty();
    }

    @Test
    void should_read_the_first_by_id_of_two_thermometers_in_one_room() {
        assertThat(Thermometers.read(List.of(shelly(140, RoomName.SANCTUM, "shellyplusht-later"),
            shelly(104, RoomName.SANCTUM, SANCTUM_HT)))).containsOnlyKeys(SANCTUM_HT);
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
