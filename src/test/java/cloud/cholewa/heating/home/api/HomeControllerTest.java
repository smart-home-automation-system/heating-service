//package cloud.cholewa.heating.home.api;
//
//import cloud.cholewa.heating.home.model.HomeConfigurationResponse;
//import cloud.cholewa.heating.home.model.RoomConfigurationResponse;
//import cloud.cholewa.heating.home.service.HomeService;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.test.web.reactive.server.WebTestClient;
//import reactor.core.publisher.Mono;
//
//import static org.mockito.ArgumentMatchers.anyString;
//
//@WebFluxTest(HomeController.class)
//class HomeControllerTest {
//
//    @Autowired
//    private WebTestClient webClient;
//
//    @MockBean
//    private HomeService homeService;
//
//    @Test
//    void should_return_ok_with_home_configuration() {
//
//        Mockito.when(homeService.getHomeConfiguration())
//            .thenReturn(Mono.just(HomeConfigurationResponse.builder().build()));
//
//        webClient.get()
//            .uri("/heating/configuration/home")
//            .exchange()
//            .expectStatus().isOk();
//    }
//
//    @Test
//    void should_return_ok_with_room_configuration_when_room_exists() {
//
//        Mockito.when(homeService.getRoomConfiguration(anyString()))
//            .thenReturn(Mono.just(RoomConfigurationResponse.builder().build()));
//
//        webClient.get()
//            .uri(uriBuilder -> uriBuilder.path("/heating/configuration/room")
//                .queryParam("roomName", "cinema")
//                .build()
//            )
//            .exchange()
//            .expectStatus().isOk();
//    }
//
//    @Test
//    void should_return_not_found_with_room_configuration_when_room_not_exists() {
//
//        Mockito.when(homeService.getRoomConfiguration(anyString()))
//            .thenReturn(Mono.empty());
//
//        webClient.get()
//            .uri(uriBuilder -> uriBuilder.path("/heating/configuration/room")
//                .queryParam("roomName", "dummy")
//                .build()
//            )
//            .exchange()
//            .expectStatus().isNotFound();
//    }
//}