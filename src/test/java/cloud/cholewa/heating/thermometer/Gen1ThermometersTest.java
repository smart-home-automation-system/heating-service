package cloud.cholewa.heating.thermometer;

import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.shelly.http.ShellyCallException;
import cloud.cholewa.shelly.http.ShellyGen1Client;
import cloud.cholewa.shelly.model.ExternalHumidityValue;
import cloud.cholewa.shelly.model.ExternalTemperatureValue;
import cloud.cholewa.shelly.model.Gen1DeviceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//the poll of the Gen1 thermometers (HAS-368): sensor 0 of the add-on handed on as a reading of now, the humidity when
//there is a DHT22, a device that does not answer or answers no temperature a failed read that stops nothing
class Gen1ThermometersTest {

    private static final String SAUNA = "192.0.2.42";
    private static final String OTHER = "192.0.2.43";

    private final Thermometers thermometers = mock(Thermometers.class);
    private final ThermometerReadings readings = mock(ThermometerReadings.class);
    private final ShellyGen1Client client = mock(ShellyGen1Client.class);
    private final Gen1Thermometers poll = new Gen1Thermometers(thermometers, readings, client,
        Clock.fixed(Instant.parse("2026-11-02T14:00:00Z"), ZoneId.of("Europe/Warsaw")));

    private static Gen1DeviceStatus status(final Double celsius, final Double humidity) {
        final Gen1DeviceStatus status = new Gen1DeviceStatus();
        if (celsius != null) {
            final ExternalTemperatureValue sensor = new ExternalTemperatureValue();
            sensor.settC(celsius);
            status.putExtTemperatureItem("0", sensor);
        }
        if (humidity != null) {
            final ExternalHumidityValue sensor = new ExternalHumidityValue();
            sensor.setHum(humidity);
            status.putExtHumidityItem("0", sensor);
        }
        return status;
    }

    @BeforeEach
    void setUp() {
        when(thermometers.gen1()).thenReturn(Map.of(SAUNA, RoomName.SAUNA));
        when(readings.temperature(any(), anyDouble(), any())).thenReturn(Mono.empty());
        when(readings.humidity(any(), anyDouble(), any())).thenReturn(Mono.empty());
    }

    @Test
    void should_hand_the_temperature_of_the_add_on_on_as_a_reading_of_now() {
        when(client.status(SAUNA)).thenReturn(Mono.just(status(7.12, null)));

        poll.poll().as(StepVerifier::create).verifyComplete();

        verify(readings).temperature(RoomName.SAUNA, 7.12, null);
        verify(readings, never()).humidity(any(), anyDouble(), any());
    }

    //a poll a minute: the same value again only after ten minutes, a change at once
    @Test
    void should_hand_the_same_value_on_only_every_ten_minutes() {
        final Instant[] now = {Instant.parse("2026-11-02T14:00:00Z")};
        final Clock clock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneId.of("Europe/Warsaw");
            }

            @Override
            public Clock withZone(final ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now[0];
            }
        };
        final Gen1Thermometers moving = new Gen1Thermometers(thermometers, readings, client, clock);
        when(client.status(SAUNA)).thenReturn(Mono.just(status(7.12, null)), Mono.just(status(7.12, null)),
            Mono.just(status(7.5, null)), Mono.just(status(7.5, null)));

        moving.poll().as(StepVerifier::create).verifyComplete();
        now[0] = now[0].plus(Duration.ofMinutes(1));
        moving.poll().as(StepVerifier::create).verifyComplete();
        now[0] = now[0].plus(Duration.ofMinutes(1));
        moving.poll().as(StepVerifier::create).verifyComplete();
        now[0] = now[0].plus(Gen1Thermometers.REPEAT_AFTER);
        moving.poll().as(StepVerifier::create).verifyComplete();

        verify(readings).temperature(RoomName.SAUNA, 7.12, null);
        verify(readings, times(2)).temperature(RoomName.SAUNA, 7.5, null);
    }

    @Test
    void should_hand_the_humidity_on_when_the_add_on_has_a_dht22() {
        when(client.status(SAUNA)).thenReturn(Mono.just(status(21.4, 48.6)));

        poll.poll().as(StepVerifier::create).verifyComplete();

        verify(readings).temperature(RoomName.SAUNA, 21.4, null);
        verify(readings).humidity(RoomName.SAUNA, 48.6, null);
    }

    //a device without its sensor answers no temperature: a failed read, nothing handed on
    @Test
    void should_take_nothing_from_a_device_without_a_temperature() {
        when(client.status(SAUNA)).thenReturn(Mono.just(status(null, null)));

        poll.poll().as(StepVerifier::create).verifyComplete();

        verify(readings, never()).temperature(any(), anyDouble(), any());
    }

    //each device with its own error handling: the next one is read
    @Test
    void should_read_the_next_device_after_one_that_fails() {
        final Map<String, RoomName> two = new LinkedHashMap<>();
        two.put(SAUNA, RoomName.SAUNA);
        two.put(OTHER, RoomName.GARDEN);
        when(thermometers.gen1()).thenReturn(two);
        when(client.status(SAUNA)).thenReturn(Mono.error(new ShellyCallException("The device did not answer")));
        when(client.status(OTHER)).thenReturn(Mono.just(status(3.5, null)));

        poll.poll().as(StepVerifier::create).verifyComplete();

        verify(readings).temperature(RoomName.GARDEN, 3.5, null);
    }

    @Test
    void should_read_nothing_without_a_gen1_thermometer() {
        when(thermometers.gen1()).thenReturn(Map.of());

        poll.poll().as(StepVerifier::create).verifyComplete();

        verify(client, never()).status(any());
    }

    @Test
    void should_end_a_poll_that_never_completes_at_its_bound() {
        doReturn(Mono.never()).when(client).status(SAUNA);

        StepVerifier.withVirtualTime(poll::poll)
            .expectSubscription()
            .expectNoEvent(Gen1Thermometers.LIMIT.minusSeconds(1))
            .thenAwait(Duration.ofSeconds(1))
            .verifyComplete();
    }
}
