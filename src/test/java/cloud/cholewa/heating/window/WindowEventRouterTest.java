package cloud.cholewa.heating.window;

import cloud.cholewa.shelly.codec.ShellyDecodeException;
import cloud.cholewa.shelly.codec.ShellyMqttDecoder;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//Only the window of a sensor the window sensors know is read off the device queue (HAS-232)
class WindowEventRouterTest {

    private final WindowSensors windowSensors = mock(WindowSensors.class);
    private final WindowEventRouter router =
        new WindowEventRouter(new ShellyMqttDecoder(JsonMapper.builder().build()), windowSensors);

    @Test
    void should_hand_the_window_of_a_known_sensor_to_the_window_sensors() {
        when(windowSensors.knows("shellyplugsg3-28372f2f0460", 215)).thenReturn(true);

        route("shellyplugsg3-28372f2f0460.status.bthomesensor:215", """
            {"id":215,"value":true,"last_updated_ts":1792235311}""");

        verify(windowSensors).observe(eq("shellyplugsg3-28372f2f0460"), eq(215),
            argThat(status -> Boolean.TRUE.equals(status.getValue()) && status.getLastUpdatedTs() == 1792235311L));
    }

    //every other bthomesensor (the weather station, a battery) and every other topic is not read at all
    @Test
    void should_not_read_what_is_not_a_known_window() {
        when(windowSensors.knows(any(), any())).thenReturn(false);

        route("shellyplugsg3-28372f2f0460.status.bthomesensor:205", "not even json");
        route("shellyplus1pm-a1b2c3.status.switch:0", "not even json");
        route("eaton.blinds.45", "not even json");
        router.route(null, new byte[0]).as(StepVerifier::create).verifyComplete();

        verify(windowSensors, never()).observe(any(), anyInt(), any());
    }

    //a body that is not what its topic carries is an error for the consumer to log
    @Test
    void should_fail_on_a_body_that_is_no_status() {
        when(windowSensors.knows("shellyplugsg3-28372f2f0460", 215)).thenReturn(true);

        router.route("shellyplugsg3-28372f2f0460.status.bthomesensor:215", "[1]".getBytes(StandardCharsets.UTF_8))
            .as(StepVerifier::create)
            .expectError(ShellyDecodeException.class)
            .verify();
    }

    private void route(final String routingKey, final String body) {
        router.route(routingKey, body.getBytes(StandardCharsets.UTF_8)).as(StepVerifier::create).verifyComplete();
    }
}
