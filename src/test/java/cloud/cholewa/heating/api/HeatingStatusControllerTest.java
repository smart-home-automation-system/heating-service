package cloud.cholewa.heating.api;

import cloud.cholewa.heating.model.HeatingStatusReply;
import cloud.cholewa.heating.service.HeatingStatusService;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;

@WebFluxTest(HeatingStatusController.class)
class HeatingStatusControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean(answers = Answers.RETURNS_SMART_NULLS)
    private HeatingStatusService heatingStatusService;

    @Test
    void queryHeatingStatusEnabled() {
        when(heatingStatusService.getHeatingStatusEnabled())
            .thenReturn(Mono.just(new HeatingStatusReply(false, LocalDateTime.now())));

        webTestClient.get()
            .exchange()
            .expectStatus().isOk()
            .expectBody(HeatingStatusReply.class);
    }

    @Test
    void updateHeatingStatusEnabled() {
        when(heatingStatusService.updateHeatingStatusEnabled("on"))
            .thenReturn(Mono.just(new HeatingStatusReply(true, LocalDateTime.now())));

        webTestClient.post()
            .uri("?turn=on")
            .exchange()
            .expectStatus().isOk()
            .expectBody(HeatingStatusReply.class);
    }
}