//package cloud.cholewa.heating.water.api;
//
//import cloud.cholewa.heating.water.db.model.HotWaterTemperatureEntity;
//import cloud.cholewa.heating.water.service.HotWaterService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;
//import reactor.core.publisher.Mono;
//
//@RestController
//@RequiredArgsConstructor
//@Slf4j
//public class HowWaterController {
//
//    private final HotWaterService hotWaterService;
//
//    @GetMapping
//    Mono<ResponseEntity<HotWaterTemperatureEntity>> getStatus() {
//        log.info("How water is running");
//        return hotWaterService.getStatus();
//    }
//}
