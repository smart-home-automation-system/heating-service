package cloud.cholewa.heating.amx.handler;

import cloud.cholewa.heating.model.Fireplace;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.pump.service.FireplacePumpService;
import cloud.cholewa.heating.room.service.RoomService;
import cloud.cholewa.home.model.DeviceStatusUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static cloud.cholewa.home.model.RoomName.BOILER;

@Slf4j
@Component
@RequiredArgsConstructor
public class TemperatureSensorHandler {

    private final Fireplace fireplace;
    private final List<Room> rooms;
    private final RoomService roomService;
    private final FireplacePumpService fireplacePumpService;

    public Mono<Void> handle(final DeviceStatusUpdate device) {
        if (device.getRoomName().equals(BOILER)) {
            return handleFireplace(device);
        }

        return Flux.fromIterable(rooms)
            .filter(room -> room.getName().equals(device.getRoomName()))
            .flatMap(room -> adjustTemperatureOffset(room, device))
            .single()
            .doOnNext(room -> logStatusUpdate(room, device))
            .doOnError(throwable -> log.error(
                "Unknown Room [{}] while handling temperature sensor",
                device.getRoomName().name(), throwable
            ))
            .flatMap(roomService::handleRoom);
    }

    private Mono<Void> handleFireplace(final DeviceStatusUpdate device) {
        fireplace.temperature().setUpdatedAt(LocalDateTime.now());
        fireplace.temperature().setValue(Double.parseDouble(device.getValue()));
        logStatusUpdate(null, device);
        return fireplacePumpService.handleFireplacePump();
    }

    private Mono<Room> adjustTemperatureOffset(final Room room, final DeviceStatusUpdate device) {
        room.getTemperature().setUpdatedAt(LocalDateTime.now());

        //adding temperature offset due to sensor issue
        switch (device.getRoomName()) {
            case OFFICE -> room.getTemperature().setValue(Double.parseDouble(device.getValue()) + 1);
            case LIVIA, TOBI -> room.getTemperature().setValue(Double.parseDouble(device.getValue()) + 0.5);
            case BATHROOM_DOWN -> room.getTemperature().setValue(Double.parseDouble(device.getValue()) + 2);
            default -> room.getTemperature().setValue(Double.parseDouble(device.getValue()));
        }
        return Mono.just(room);
    }

    private void logStatusUpdate(final Room room,final DeviceStatusUpdate deviceStatusUpdate) {
        log.info(
            "Status update, room: [{}], device: [{}], value: [{}]",
            deviceStatusUpdate.getRoomName().equals(BOILER) ? "FIREPLACE" : deviceStatusUpdate.getRoomName().name(),
            deviceStatusUpdate.getDeviceType().name(),
            room == null ? deviceStatusUpdate.getValue() : room.getTemperature().getValue()
        );
    }
}
