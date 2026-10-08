package cloud.cholewa.heating.api;

import cloud.cholewa.heating.model.FloorPumpReply;
import cloud.cholewa.heating.service.FloorPumpService;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;

@WebFluxTest(FloorPumpController.class)
class FloorPumpControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean(answers = Answers.RETURNS_SMART_NULLS)
    private FloorPumpService floorPumpService;

    @Test
    void should_return_the_state_of_the_floor_pump() {
        when(floorPumpService.queryFloorPump())
            .thenReturn(Mono.just(new FloorPumpReply(false, LocalDateTime.of(2026, 10, 8, 18, 31))));

        webTestClient.get()
            .uri("/floor-pump")
            .exchange()
            .expectStatus().isOk()
            .expectBody().json("""
                {"working": false, "updatedAt": "2026-10-08T18:31:00"}
                """, JsonCompareMode.STRICT);
    }

    //before the relay has answered: an empty object, not "working: false"
    @Test
    void should_return_an_empty_object_before_the_pump_has_reported() {
        when(floorPumpService.queryFloorPump()).thenReturn(Mono.just(new FloorPumpReply(null, null)));

        webTestClient.get()
            .uri("/floor-pump")
            .exchange()
            .expectStatus().isOk()
            .expectBody().json("{}", JsonCompareMode.STRICT);
    }
}
