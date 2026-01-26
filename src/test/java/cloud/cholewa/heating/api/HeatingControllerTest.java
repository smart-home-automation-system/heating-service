package cloud.cholewa.heating.api;

import cloud.cholewa.heating.service.HeatingService;
import cloud.cholewa.home.model.SystemActiveReply;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;

@WebFluxTest(HeatingController.class)
class HeatingControllerTest {

    @MockitoBean(answers = Answers.RETURNS_SMART_NULLS)
    private HeatingService heatingService;
    
    @Autowired
    private WebTestClient webTestClient;

    @Test
    void should_return_active_status() {
        when(heatingService.queryHeatingSystemActive())
            .thenReturn(Mono.just(SystemActiveReply.builder().active(true).build()));

        webTestClient.get()
            .uri("/status/active")
            .exchange()
            .expectStatus().isOk();
    }
}
