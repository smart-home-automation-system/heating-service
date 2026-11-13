package cloud.cholewa.heating.client;

import cloud.cholewa.heating.config.RelayConfig;
import cloud.cholewa.heating.config.ShellyConfig;
import cloud.cholewa.heating.infrastructure.error.BoilerException;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.shelly.http.ShellyGen1Client;
import cloud.cholewa.shelly.http.ShellyTimeouts;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ShellyClientTest {

    private MockWebServer mockWebServer;
    private ShellyClient sut;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        RelayConfig relayConfig = new RelayConfig();

        ReflectionTestUtils.setField(
            relayConfig,
            "heater", Map.of("bedroom", 1, "living_room", 2, "cinema", 3)
        );
        //HAS-361: the radiator of sanctum is a Gen1 plug of its own
        ReflectionTestUtils.setField(relayConfig, "standalone",
            Map.of("sanctum", mockWebServer.getHostName() + ":" + mockWebServer.getPort()));

        ShellyConfig shellyConfig = new ShellyConfig(relayConfig);
        ReflectionTestUtils.setField(shellyConfig, "scheme", "http");
        ReflectionTestUtils.setField(shellyConfig, "port", mockWebServer.getPort());
        ReflectionTestUtils.setField(shellyConfig, "SHELLY_PRO4_DOWN_RIGHT", mockWebServer.getHostName());
        ReflectionTestUtils.setField(shellyConfig, "SHELLY_PRO4_UP_LEFT", mockWebServer.getHostName());

        sut = new ShellyClient(WebClient.create(), shellyConfig, relayConfig, new ShellyGen1Client(
            WebClient.builder(), new ShellyTimeouts(Duration.ofSeconds(3),
            Duration.ofSeconds(5))));
    }

    @AfterEach
    @SneakyThrows
    void tearDown() {
        mockWebServer.shutdown();
    }

    @Test
    void should_get_heater_actor_status_successfully() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(HttpStatus.OK.value())
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("""
                {
                    "output": false
                }
                """)
        );

        sut.getHeaterActorStatus(HeaterType.RADIATOR, RoomName.CINEMA)
            .as(StepVerifier::create)
            .expectNextMatches(status -> Boolean.FALSE.equals(status.getOutput()))
            .verifyComplete();
    }

    @Test
    void should_handle_error_when_getting_heater_actor_status() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
        );

        sut.getHeaterActorStatus(HeaterType.RADIATOR, RoomName.CINEMA)
            .as(StepVerifier::create)
            .expectErrorMatches(BoilerException.class::isInstance)
            .verify();
    }

    @Test
    void should_control_heater_actor_successfully() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(HttpStatus.OK.value())
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("""
                {
                    "ison": true
                }
                """)
        );

        sut.controlHeaterActor(HeaterType.RADIATOR, RoomName.CINEMA, true)
            .as(StepVerifier::create)
            .expectNextMatches(status -> Boolean.TRUE.equals(status.getIson()))
            .verifyComplete();
    }

    @Test
    void should_handle_error_when_controlling_heater_actor() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
        );

        sut.controlHeaterActor(HeaterType.RADIATOR, RoomName.CINEMA, true)
            .as(StepVerifier::create)
            .expectErrorMatches(BoilerException.class::isInstance)
            .verify();
    }

    @Test
    void should_get_floor_pump_status_successfully() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(HttpStatus.OK.value())
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("""
                {
                    "output": true
                }
                """)
        );

        sut.getFloorPumpStatus()
            .as(StepVerifier::create)
            .expectNextMatches(status -> Boolean.TRUE.equals(status.getOutput()))
            .verifyComplete();
    }

    @Test
    void should_handle_error_when_getting_floor_pump_status() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
        );

        sut.getFloorPumpStatus()
            .as(StepVerifier::create)
            .expectErrorMatches(BoilerException.class::isInstance)
            .verify();
    }

    @Test
    void should_control_floor_pump_successfully() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(HttpStatus.OK.value())
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("""
                {
                    "ison": true
                }
                """)
        );

        sut.controlFloorPump(true)
            .as(StepVerifier::create)
            .expectNextMatches(status -> Boolean.TRUE.equals(status.getIson()))
            .verifyComplete();
    }

    @Test
    void should_handle_error_when_controlling_floor_pump() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
        );

        sut.controlFloorPump(true)
            .as(StepVerifier::create)
            .expectErrorMatches(BoilerException.class::isInstance)
            .verify();
    }

    //HAS-361: the standalone radiator is read as a Gen1 plug - /relay/0 - and told as a Pro 4 relay
    @Test
    @SneakyThrows
    void should_read_the_standalone_radiator_as_a_gen1_relay() {
        mockWebServer.enqueue(new MockResponse()
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"ison\": true, \"has_timer\": false, \"source\": \"http\"}"));

        sut.getHeaterActorStatus(HeaterType.RADIATOR, RoomName.SANCTUM)
            .as(StepVerifier::create)
            .expectNextMatches(status -> Boolean.TRUE.equals(status.getOutput()))
            .verifyComplete();

        assertThat(mockWebServer.takeRequest().getPath()).isEqualTo("/relay/0");
    }

    @Test
    @SneakyThrows
    void should_switch_the_standalone_radiator_as_a_gen1_relay() {
        mockWebServer.enqueue(new MockResponse()
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"ison\": false, \"has_timer\": false, \"source\": \"http\"}"));

        sut.controlHeaterActor(HeaterType.RADIATOR, RoomName.SANCTUM, false)
            .as(StepVerifier::create)
            .expectNextMatches(relay -> Boolean.FALSE.equals(relay.getIson()))
            .verifyComplete();

        assertThat(mockWebServer.takeRequest().getPath()).isEqualTo("/relay/0?turn=off");
    }

    //a plug that answers no state is a failed call, never read as off
    @Test
    void should_fail_on_a_standalone_radiator_that_answers_no_state() {
        mockWebServer.enqueue(new MockResponse()
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{}"));

        sut.getHeaterActorStatus(HeaterType.RADIATOR, RoomName.SANCTUM)
            .as(StepVerifier::create)
            .expectErrorMatches(BoilerException.class::isInstance)
            .verify();
    }
}
