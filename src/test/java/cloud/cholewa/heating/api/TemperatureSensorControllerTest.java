package cloud.cholewa.heating.api;

import cloud.cholewa.heating.model.TemperatureSensorReply;
import cloud.cholewa.heating.service.TemperatureSensorService;
import cloud.cholewa.home.model.RoomName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;

@WebFluxTest(TemperatureSensorController.class)
class TemperatureSensorControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean(answers = Answers.RETURNS_SMART_NULLS)
    private TemperatureSensorService temperatureSensorService;

    @Test
    void queryTemperatureSensors() {
        when(temperatureSensorService.querySensors()).thenReturn(Flux.just(
            new TemperatureSensorReply(RoomName.OFFICE, LocalDateTime.of(2026, 10, 5, 11, 30), false, false),
            new TemperatureSensorReply(RoomName.LIVING_ROOM, LocalDateTime.of(2026, 10, 3, 8, 0), true, true)
        ));

        webTestClient.get()
            .uri("/temperature/sensors")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.length()").isEqualTo(2)
            .jsonPath("$[0].room").isEqualTo("office")
            .jsonPath("$[0].lastReadingAt").isEqualTo("2026-10-05T11:30:00")
            .jsonPath("$[0].stale").isEqualTo(false)
            .jsonPath("$[1].room").isEqualTo("living room")
            .jsonPath("$[0].muted").isEqualTo(false)
            .jsonPath("$[1].stale").isEqualTo(true)
            .jsonPath("$[1].muted").isEqualTo(true);
    }

    @Test
    void queryTemperatureSensors_withoutAnyReading() {
        when(temperatureSensorService.querySensors()).thenReturn(Flux.empty());

        webTestClient.get()
            .uri("/temperature/sensors")
            .exchange()
            .expectStatus().isOk()
            .expectBody().json("[]");
    }
}
