package cloud.cholewa.heating.thermometer;

import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Humidity;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.Temperature;
import cloud.cholewa.heating.service.TemperatureService;
import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.home.model.TemperatureMessage;
import cloud.cholewa.shelly.codec.ShellyDecodeException;
import cloud.cholewa.shelly.codec.ShellyMqttDecoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//the readings of a Shelly thermometer on the device queue (HAS-360), as the Plus H&T of sanctum publishes them at a
//wake-up: the temperature a reading of the room - stored and decided - and the humidity of the room in memory
class ThermometerEventRouterTest {

    private static final String HT = "shellyplusht-b0b21c138428";
    private static final ZoneId HOUSE = ZoneId.of("Europe/Warsaw");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-01T13:30:00Z"), HOUSE);

    private final Thermometers thermometers = mock(Thermometers.class);
    private final TemperatureService temperatureService = mock(TemperatureService.class);
    private final Room sanctum = Room.builder().name(RoomName.SANCTUM).temperature(Temperature.builder().build())
        .humidity(new Humidity()).build();
    private final ThermometerEventRouter router = new ThermometerEventRouter(
        new ShellyMqttDecoder(JsonMapper.builder().build()), thermometers, temperatureService,
        new Home(List.of(sanctum)), CLOCK);

    @BeforeEach
    void setUp() {
        when(thermometers.roomOf(anyString())).thenReturn(Optional.empty());
        when(thermometers.roomOf(HT)).thenReturn(Optional.of(RoomName.SANCTUM));
        when(temperatureService.handleTemperature(any())).thenReturn(Mono.empty());
    }

    private static byte[] body(final String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }

    //DoD: a Shelly reading of sanctum is stored as a reading of the room, with the time of the house
    @Test
    void should_hand_the_temperature_of_a_known_thermometer_to_the_room() {
        router.route(HT + ".status.temperature:0", body("{\"id\":0,\"tC\":21.4,\"tF\":70.5}"))
            .as(StepVerifier::create).verifyComplete();

        verify(temperatureService).handleTemperature(new TemperatureMessage().room(RoomName.SANCTUM).temperature(21.4)
            .date(LocalDateTime.of(2026, 11, 1, 14, 30)));
    }

    //DoD: the humidity answered - the first one the service has
    @Test
    void should_keep_the_humidity_of_the_room() {
        router.route(HT + ".status.humidity:0", body("{\"id\":0,\"rh\":48.6}"))
            .as(StepVerifier::create).verifyComplete();

        assertThat(sanctum.getHumidity().snapshot().getValue()).isEqualTo(48.6);
        assertThat(sanctum.getHumidity().snapshot().getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 11, 1, 14, 30));
        verify(temperatureService, never()).handleTemperature(any());
    }

    //the body of another device, or of another component, is never read - a broken body there fails nothing
    @Test
    void should_never_read_an_event_of_another_device_or_component() {
        router.route("shellyplusht-000000000000.status.temperature:0", body("not json"))
            .as(StepVerifier::create).verifyComplete();
        router.route(HT + ".status.devicepower:0", body("not json")).as(StepVerifier::create).verifyComplete();
        router.route(HT + ".status.temperature:1", body("not json")).as(StepVerifier::create).verifyComplete();
        router.route(null, new byte[0]).as(StepVerifier::create).verifyComplete();

        verify(temperatureService, never()).handleTemperature(any());
    }

    //a sensor in error answers no value: nothing is stored
    @Test
    void should_skip_a_status_without_a_value() {
        router.route(HT + ".status.temperature:0", body("{\"id\":0,\"tC\":null,\"errors\":[\"read\"]}"))
            .as(StepVerifier::create).verifyComplete();

        verify(temperatureService, never()).handleTemperature(any());
    }

    //a body that is not its topic is an error for the consumer to log
    @Test
    void should_fail_on_a_body_that_is_not_its_topic() {
        router.route(HT + ".status.temperature:0", body("[1,2]"))
            .as(StepVerifier::create).verifyError(ShellyDecodeException.class);
    }
}
