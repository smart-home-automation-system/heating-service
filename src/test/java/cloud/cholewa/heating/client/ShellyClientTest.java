package cloud.cholewa.heating.client;

import cloud.cholewa.heating.config.RelayConfig;
import cloud.cholewa.heating.config.ShellyConfig;
import cloud.cholewa.heating.infrastructure.error.BoilerException;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.home.model.RoomName;
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

import java.util.Map;

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
            "heater", Map.of("bedroom", 1, "living_room", 2, "cinema", 3));
        
        ShellyConfig shellyConfig = new ShellyConfig(relayConfig);
        ReflectionTestUtils.setField(shellyConfig, "scheme", "http");
        ReflectionTestUtils.setField(shellyConfig, "port", mockWebServer.getPort());
        ReflectionTestUtils.setField(shellyConfig, "SHELLY_PRO4_DOWN_RIGHT", mockWebServer.getHostName());

        sut = new ShellyClient(WebClient.create(), shellyConfig);
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
}