package cloud.cholewa.heating.config;

import cloud.cholewa.heating.infrastructure.error.HeatingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static cloud.cholewa.heating.model.HeaterType.FLOOR;
import static cloud.cholewa.heating.model.HeaterType.RADIATOR;
import static cloud.cholewa.home.model.RoomName.BATHROOM_UP;
import static cloud.cholewa.home.model.RoomName.BEDROOM;
import static cloud.cholewa.home.model.RoomName.LIVING_ROOM;
import static cloud.cholewa.home.model.RoomName.LOFT;
import static cloud.cholewa.home.model.RoomName.OFFICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShellyConfigTest {

    @Mock
    private RelayConfig relayConfig;

    @InjectMocks
    private ShellyConfig sut;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sut, "scheme", "http");
        ReflectionTestUtils.setField(sut, "port", 80);
        ReflectionTestUtils.setField(sut, "SHELLY_PRO4_DOWN_LEFT", "10.78.30.39");
        ReflectionTestUtils.setField(sut, "SHELLY_PRO4_DOWN_RIGHT", "10.78.30.19");
        ReflectionTestUtils.setField(sut, "SHELLY_PRO4_UP_LEFT", "10.78.30.89");
        ReflectionTestUtils.setField(sut, "SHELLY_PRO4_UP_RIGHT", "10.78.30.26");
    }

    @Test
    void should_get_status_uri_for_radiator() {
        when(relayConfig.getHeater(BEDROOM)).thenReturn(0);

        URI uri = sut.getStatusUri(UriComponentsBuilder.newInstance(), RADIATOR, BEDROOM);

        assertThat(uri.toString()).hasToString("http://10.78.30.26:80/rpc/Switch.GetStatus?id=0");
    }

    @Test
    void should_get_status_uri_for_floor() {
        when(relayConfig.getFloor(BATHROOM_UP)).thenReturn(1);

        URI uri = sut.getStatusUri(UriComponentsBuilder.newInstance(), FLOOR, BATHROOM_UP);

        assertThat(uri.toString()).hasToString("http://10.78.30.89:80/rpc/Switch.GetStatus?id=1");
    }

    @Test
    void should_throw_exception_when_getting_status_uri_for_unknown_radiator_room() {
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();

        assertThatThrownBy(() -> sut.getStatusUri(uriComponentsBuilder, RADIATOR, LOFT))
            .isInstanceOf(HeatingException.class)
            .hasMessageContaining("Unknown configuration for room heater: LOFT");
    }

    @Test
    void should_get_control_uri_for_radiator_on() {
        when(relayConfig.getHeater(OFFICE)).thenReturn(3);

        URI uri = sut.getControlUri(UriComponentsBuilder.newInstance(), RADIATOR, OFFICE, true);

        assertThat(uri.toString()).hasToString("http://10.78.30.26:80/relay/3?turn=on");
    }

    @Test
    void should_get_control_uri_for_floor_off() {
        when(relayConfig.getFloor(LIVING_ROOM)).thenReturn(0);

        URI uri = sut.getControlUri(UriComponentsBuilder.newInstance(), FLOOR, LIVING_ROOM, false);

        assertThat(uri.toString()).hasToString("http://10.78.30.39:80/relay/0?turn=off");
    }

    @Test
    void should_throw_exception_when_getting_control_uri_for_unknown_floor_room() {
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();

        assertThatThrownBy(() -> sut.getControlUri(uriComponentsBuilder, FLOOR, LOFT, true))
            .isInstanceOf(HeatingException.class)
            .hasMessageContaining("Unknown configuration for room floor: LOFT");
    }
}