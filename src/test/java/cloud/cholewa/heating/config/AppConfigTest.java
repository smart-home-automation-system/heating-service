package cloud.cholewa.heating.config;

import cloud.cholewa.heating.client.ShellyClient;
import cloud.cholewa.heating.infrastructure.error.BoilerException;
import io.netty.channel.ChannelOption;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.zalando.logbook.Logbook;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AppConfigTest {

    private final AppConfig sut = new AppConfig();

    private ConnectionProvider connectionProvider;

    @BeforeEach
    void setUp() {
        connectionProvider = sut.connectionProvider();
    }

    @AfterEach
    void tearDown() {
        connectionProvider.dispose();
    }

    @Test
    void should_build_the_shelly_client_with_the_configured_timeouts() {
        HttpClient httpClient = sut.httpClient(
            connectionProvider, Logbook.create(), Duration.ofSeconds(10), Duration.ofSeconds(5));

        assertThat(httpClient.configuration().responseTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(httpClient.configuration().options().get(ChannelOption.CONNECT_TIMEOUT_MILLIS))
            .isEqualTo(10_000);
    }

    //a relay that accepts the request and never answers must not hold its message for good
    @Test
    @SneakyThrows
    void should_end_a_call_to_a_silent_device_as_a_device_error() {
        try (MockWebServer mockWebServer = new MockWebServer()) {
            mockWebServer.start();
            mockWebServer.enqueue(new MockResponse()
                .setHeadersDelay(5, TimeUnit.SECONDS)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"output\": false}"));

            HttpClient httpClient = sut.httpClient(
                connectionProvider, Logbook.create(), Duration.ofSeconds(10), Duration.ofMillis(300));
            ShellyClient shellyClient = new ShellyClient(
                sut.shellyWebClient(WebClient.builder(), httpClient), shellyConfig(mockWebServer));

            Duration elapsed = shellyClient.getFloorPumpStatus()
                .as(StepVerifier::create)
                .expectError(BoilerException.class)
                .verify(Duration.ofSeconds(3));

            //the request did reach the device, and the call ended on the timeout - not at once,
            //as a refused connection would, and not after the device's five seconds
            assertThat(mockWebServer.takeRequest(1, TimeUnit.SECONDS)).isNotNull();
            assertThat(elapsed).isBetween(Duration.ofMillis(300), Duration.ofSeconds(3));
        }
    }

    private ShellyConfig shellyConfig(final MockWebServer mockWebServer) {
        ShellyConfig shellyConfig = new ShellyConfig(new RelayConfig());
        ReflectionTestUtils.setField(shellyConfig, "scheme", "http");
        ReflectionTestUtils.setField(shellyConfig, "port", mockWebServer.getPort());
        ReflectionTestUtils.setField(shellyConfig, "SHELLY_PRO4_UP_LEFT", mockWebServer.getHostName());
        return shellyConfig;
    }
}
