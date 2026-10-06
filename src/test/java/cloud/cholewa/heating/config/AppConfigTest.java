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
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AppConfigTest {

    private final AppConfig sut = new AppConfig();

    private MockWebServer mockWebServer;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    @SneakyThrows
    void tearDown() {
        mockWebServer.shutdown();
    }

    //a relay that never answers must not hold its message for good
    @Test
    void should_bound_the_connect_and_the_response_time_of_a_shelly_call() {
        HttpClient httpClient = sut.httpClient(sut.connectionProvider(), Logbook.create());

        assertThat(httpClient.configuration().responseTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(httpClient.configuration().options().get(ChannelOption.CONNECT_TIMEOUT_MILLIS))
            .isEqualTo(10_000);
    }

    //the configured client with the timeout shortened, so the test does not wait five seconds
    @Test
    void should_end_a_call_to_a_silent_device_as_a_device_error() {
        HttpClient httpClient = sut.httpClient(sut.connectionProvider(), Logbook.create())
            .responseTimeout(Duration.ofMillis(200));
        ShellyClient shellyClient = new ShellyClient(sut.shellyWebClient(WebClient.builder(), httpClient), shellyConfig());

        mockWebServer.enqueue(new MockResponse()
            .setHeadersDelay(5, TimeUnit.SECONDS)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"output\": false}"));

        shellyClient.getFloorPumpStatus()
            .as(StepVerifier::create)
            .expectError(BoilerException.class)
            .verify(Duration.ofSeconds(3));
    }

    private ShellyConfig shellyConfig() {
        ShellyConfig shellyConfig = new ShellyConfig(new RelayConfig());
        ReflectionTestUtils.setField(shellyConfig, "scheme", "http");
        ReflectionTestUtils.setField(shellyConfig, "port", mockWebServer.getPort());
        ReflectionTestUtils.setField(shellyConfig, "SHELLY_PRO4_DOWN_RIGHT", mockWebServer.getHostName());
        ReflectionTestUtils.setField(shellyConfig, "SHELLY_PRO4_UP_LEFT", mockWebServer.getHostName());
        return shellyConfig;
    }
}
