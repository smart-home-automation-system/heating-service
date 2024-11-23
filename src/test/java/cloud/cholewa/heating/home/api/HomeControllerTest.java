package cloud.cholewa.heating.home.api;

import cloud.cholewa.heating.home.api.model.HomeShortResponse;
import cloud.cholewa.heating.home.service.HomeService;
import cloud.cholewa.heating.infrastructure.error.ExceptionHandlerConfig;
import cloud.cholewa.heating.infrastructure.error.RoomNotFoundException;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.home.model.RoomName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(HomeController.class)
@Import(ExceptionHandlerConfig.class)
class HomeControllerTest {

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private HomeService homeService;
    @MockBean
    private Home home;

    @Test
    void should_return_home_configuration() {
        when(homeService.getHomeStatus()).thenReturn(Mono.just(home));

        webClient.get().uri("/status")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void should_return_short_home_configuration() {
        when(homeService.getHomeShortStatus()).thenReturn(Mono.just(HomeShortResponse.builder().build()));

        webClient.get().uri("/status:short")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void should_return_boiler_room_configuration() {

        webClient.get().uri("/status/boiler")
            .exchange()
            .expectStatus().is5xxServerError();
    }

    @Test
    void should_return_room_configuration_when_room_is_valid() {
        when(homeService.getRoomStatusByName("office"))
            .thenReturn(Mono.just(Room.builder().name(RoomName.OFFICE).build()));

        webClient.get().uri("/status/room/office")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void should_return_error_room_not_found_when_room_is_not_valid() {
        when(homeService.getRoomStatusByName(anyString()))
            .thenReturn(Mono.error(new RoomNotFoundException("ddd")));

        webClient.get().uri("/status/room/office")
            .exchange()
            .expectStatus().isNotFound();
    }
}