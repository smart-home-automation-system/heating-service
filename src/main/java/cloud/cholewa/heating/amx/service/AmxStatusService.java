package cloud.cholewa.heating.amx.service;

import cloud.cholewa.heating.amx.handler.TemperatureSensorHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmxStatusService {

    private final TemperatureSensorHandler temperatureSensorHandler;

//    public Mono<Void> updateStatus(final DeviceStatusUpdate deviceStatus) {
//        return Mono.just(deviceStatus)
//            .flatMap(device -> switch (device.getDeviceType()) {
//                case TEMPERATURE_SENSOR -> temperatureSensorHandler.handle(device);
//                default -> Mono.error(new DeviceException("Unknown device type: " + device.getDeviceType()));
//            })
//            .then();
//    }
}
