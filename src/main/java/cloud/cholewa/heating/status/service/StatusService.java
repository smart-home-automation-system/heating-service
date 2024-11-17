package cloud.cholewa.heating.status.service;

import cloud.cholewa.heating.air.service.AirHeatingService;
import cloud.cholewa.heating.model.HeatingSource;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.TemperatureSensor;
import cloud.cholewa.heating.shelly.pro.service.ShellyProService;
import cloud.cholewa.home.model.DeviceStatusUpdate;
import cloud.cholewa.home.model.RoomName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static cloud.cholewa.heating.model.HeatingSourceType.FIREPLACE;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatusService {

    private final Home home;
    private final ShellyProService shellyProService;
    private final AirHeatingService airHeatingService;

    public Mono<ResponseEntity<Void>> updateStatusFromAmx(final DeviceStatusUpdate deviceStatus) {
        updateStatus(deviceStatus);

        return Mono.just(ResponseEntity.ok().build());
    }

    private void updateStatus(final DeviceStatusUpdate deviceStatus) {
        if (deviceStatus.getRoomName().equals(RoomName.BOILER)) {
            final HeatingSource fireplace = home.getBoiler().getHeatingSources().stream()
                .filter(heatingSource -> heatingSource.getType().equals(FIREPLACE))
                .findFirst().orElseThrow();

            fireplace.setUpdateTime(LocalDateTime.now());
            fireplace.setTemperature(Double.parseDouble(deviceStatus.getValue()));

            shellyProService.controlFirePlacePump(fireplace.getTemperature())
                .doOnNext(fireplace::setActive)
                .subscribe();
        } else {
            final Room roomUpdate = home.getRooms().stream()
                .filter(room -> room.getName().equals(deviceStatus.getRoomName()))
                .findFirst().orElseThrow();

            roomUpdate.setTemperatureSensor(TemperatureSensor.builder()
                .updateTime(LocalDateTime.now())
                .temperature(roomUpdate.getName().equals(RoomName.BATHROOM_DOWN)
                    ? Double.parseDouble(deviceStatus.getValue()) + 3 //due to sensor failure temp inc by 3
                    : Double.parseDouble(deviceStatus.getValue()))
                .build());

            airHeatingService.controlHeaterSource(roomUpdate);
        }
    }
}
