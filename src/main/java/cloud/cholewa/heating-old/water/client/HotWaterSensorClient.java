//package cloud.cholewa.heating.water.client;
//
//import cloud.cholewa.shelly.model.ShellyUniStatusResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Component;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.core.publisher.Mono;
//
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class HotWaterSensorClient {
//
//    private final HotWaterSensorConfig config;
//    private final WebClient hotWaterSensorClient;
//
//    public Mono<ResponseEntity<ShellyUniStatusResponse>> getStatus() {
//        log.info("Getting status of hot-water sensor");
//        return hotWaterSensorClient
//            .get()
//            .uri(uriBuilder -> config
//                .getUriBuilder(uriBuilder)
//                .path("status")
//                .build()
//            )
//            .retrieve()
//            .toEntity(ShellyUniStatusResponse.class);
//    }
//}
