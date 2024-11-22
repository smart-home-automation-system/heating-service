//package cloud.cholewa.heating.status.api;
//
//import cloud.cholewa.heating.status.service.StatusService;
//import cloud.cholewa.home.model.DeviceStatusUpdate;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import reactor.core.publisher.Mono;
//
//@Slf4j
//@RestController
//@RequestMapping("/heating/device")
//@RequiredArgsConstructor
//public class StatusController {
//
//    private final StatusService statusService;
//
//    @PostMapping("/amx/status:update")
//    Mono<ResponseEntity<Void>> receiveStatusFromAmx(@Valid @RequestBody final DeviceStatusUpdate deviceStatusUpdate) {
//        log.info(
//            "Received device status update for room: [{}], device: [{}], with value: [{}]",
//            deviceStatusUpdate.getRoomName().name(),
//            deviceStatusUpdate.getDeviceType().name(),
//            deviceStatusUpdate.getValue()
//        );
//
//        return statusService.updateStatusFromAmx(deviceStatusUpdate);
//    }
//}
