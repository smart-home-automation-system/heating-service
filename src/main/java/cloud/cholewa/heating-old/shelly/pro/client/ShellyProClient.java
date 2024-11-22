//package cloud.cholewa.heating.shelly.pro.client;
//
//import cloud.cholewa.shelly.model.ShellyProRelayResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.core.publisher.Mono;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class ShellyProClient {
//
//    private static final String SHELLY_PRO4_BOILER_IP = "10.78.30.20";
//    private final WebClient webClient;
//
//    public Mono<ShellyProRelayResponse> controlFireplacePump(final boolean enable) {
//        return webClient.get()
//            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_BOILER_IP)
//                .path("relay/3")
//                .queryParam("turn", enable ? "on" : "off")
//                .build())
//            .retrieve()
//            .bodyToMono(ShellyProRelayResponse.class);
//    }
//
//    public Mono<ShellyProRelayResponse> controlFurnace(final boolean enable) {
//        return webClient.get()
//            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_BOILER_IP)
//                .path("relay/0")
//                .queryParam("turn", enable ? "on" : "off")
//                .build())
//            .retrieve()
//            .bodyToMono(ShellyProRelayResponse.class);
//    }
//
//    public Mono<ShellyProRelayResponse> controlHotWaterPump(final boolean enable) {
//        return webClient.get()
//            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_BOILER_IP)
//                .path("relay/1")
//                .queryParam("turn", enable ? "on" : "off")
//                .build())
//            .retrieve()
//            .bodyToMono(ShellyProRelayResponse.class);
//    }
//
//    public Mono<ShellyProRelayResponse> controlHeatingPump(final boolean enable) {
//        return webClient.get()
//            .uri(uriBuilder -> uriBuilder.scheme("http").host(SHELLY_PRO4_BOILER_IP)
//                .path("relay/2")
//                .queryParam("turn", enable ? "on" : "off")
//                .build())
//            .retrieve()
//            .bodyToMono(ShellyProRelayResponse.class);
//    }
//}
