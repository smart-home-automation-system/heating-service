package cloud.cholewa.heating.amx.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/heating/device")
@RequiredArgsConstructor
public class AmxStatusController {

//    private final AmxStatusService amxStatusService;

//    @PostMapping("/amx/status:update")
//    Mono<ResponseEntity<Void>> receiveStatusFromAmx(@Valid @RequestBody final DeviceStatusUpdate deviceStatusUpdate) {
//        return amxStatusService.updateStatus(deviceStatusUpdate).then(Mono.just(ResponseEntity.ok().build()));
//    }
}
