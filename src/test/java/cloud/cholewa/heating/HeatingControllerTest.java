package cloud.cholewa.heating;

import cloud.cholewa.heating.api.HeatingController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(HeatingController.class)
class HeatingControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void should_return_active_status() {

        webTestClient.get()
            .uri("/status/active")
            .exchange()
            .expectStatus().isOk();
    }
}
