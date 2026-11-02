package cloud.cholewa.heating.thermometer;

import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.shelly.model.BthomeSensorStatus;
import cloud.cholewa.shelly.model.ShellyComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

//which bthomesensor of which gateway is what of which BLU thermometer (HAS-364)
class BluThermometersTest {

    private static final String GATEWAY = "shellypresence-ccba97ee868c";
    private static final String OTHER_GATEWAY = "shellyplus1pm-000000000000";
    private static final String BATH_UP = "38:39:8f:86:5f:bf";

    private final Thermometers thermometers = mock(Thermometers.class);
    private final BluThermometers sut = new BluThermometers(thermometers, JsonMapper.builder().build());

    @BeforeEach
    void setUp() {
        when(thermometers.bluRoomOf(any())).thenReturn(Optional.empty());
        when(thermometers.bluRoomOf(BATH_UP)).thenReturn(Optional.of(RoomName.BATHROOM_UP));
    }

    private static ShellyComponent component(final String key, final Map<String, Object> config, final Map<String, Object> status) {
        return ShellyComponent.builder().key(key).config(new HashMap<>(config)).status(new HashMap<>(status)).build();
    }

    private static ShellyComponent sensor(final int id, final String address, final int object, final Object value) {
        final Map<String, Object> status = new HashMap<>();
        status.put("id", id);
        status.put("value", value);
        status.put("last_updated_ts", 1793554189L);
        return component("bthomesensor:" + id, Map.of("id", id, "addr", address, "obj_id", object, "idx", 0), status);
    }

    //the temperature and the humidity of a thermometer of the registry; its battery, a window and another device are not
    @Test
    void should_map_the_temperature_and_the_humidity_of_a_registered_thermometer() {
        final List<BluThermometers.Reading> readings = sut.apply(Map.of(GATEWAY.toUpperCase(), List.of(
            sensor(203, BATH_UP, 0x01, 100),
            sensor(204, BATH_UP, 0x2E, 64),
            sensor(205, BATH_UP, 0x45, 23.8),
            sensor(202, "b0:c7:de:3e:93:14", 0x45, 21.0),
            sensor(206, "7c:c6:b6:92:b5:36", 0x2D, true),
            component("bthomedevice:201", Map.of("id", 201, "addr", BATH_UP), Map.of("id", 201)),
            component("presencezone:200", Map.of("id", 200), Map.of("id", 200, "value", true)))));

        assertThat(readings).containsExactlyInAnyOrder(
            new BluThermometers.Reading(RoomName.BATHROOM_UP, BluThermometers.Metric.HUMIDITY, 64,
                Instant.ofEpochSecond(1793554189L)),
            new BluThermometers.Reading(RoomName.BATHROOM_UP, BluThermometers.Metric.TEMPERATURE, 23.8,
                Instant.ofEpochSecond(1793554189L)));
        assertThat(sut.target(GATEWAY, 205)).contains(
            new BluThermometers.Target(RoomName.BATHROOM_UP, BluThermometers.Metric.TEMPERATURE, BATH_UP));
        assertThat(sut.target(GATEWAY, 203)).isEmpty();
        assertThat(sut.target(GATEWAY, 202)).isEmpty();
        assertThat(sut.target(null, 205)).isEmpty();
        assertThat(sut.target(GATEWAY, null)).isEmpty();
    }

    //a thermometer moved to another gateway, or a gateway gone from the registry, leaves no component behind
    @Test
    void should_build_the_map_anew_from_every_read() {
        sut.apply(Map.of(GATEWAY, List.of(sensor(205, BATH_UP, 0x45, 23.8))));
        sut.apply(Map.of(OTHER_GATEWAY, List.of(sensor(210, BATH_UP, 0x45, 23.9))));

        assertThat(sut.target(GATEWAY, 205)).isEmpty();
        assertThat(sut.target(OTHER_GATEWAY, 210)).isPresent();
    }

    //a sensor not heard since the gateway started holds no value: no reading
    @Test
    void should_give_no_reading_without_a_value() {
        final BluThermometers.Target target =
            new BluThermometers.Target(RoomName.BATHROOM_UP, BluThermometers.Metric.TEMPERATURE, BATH_UP);

        assertThat(BluThermometers.reading(target, BthomeSensorStatus.builder().id(205).lastUpdatedTs(0L).build())).isEmpty();
        assertThat(BluThermometers.reading(target, BthomeSensorStatus.builder().id(205).value(true).build())).isEmpty();
        assertThat(BluThermometers.reading(target, BthomeSensorStatus.builder().id(205).value(23.8).lastUpdatedTs(0L).build()))
            .contains(new BluThermometers.Reading(RoomName.BATHROOM_UP, BluThermometers.Metric.TEMPERATURE, 23.8, null));
    }
}
