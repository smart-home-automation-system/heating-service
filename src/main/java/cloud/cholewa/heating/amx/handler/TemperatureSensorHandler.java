package cloud.cholewa.heating.amx.handler;

import cloud.cholewa.heating.model.BoilerRoom;
import cloud.cholewa.heating.model.Fireplace;
import cloud.cholewa.heating.model.Room;
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

    private final BoilerRoom boilerRoom;
    private final Fireplace fireplace;
    private final List<Room> rooms;

    public Mono<Void> handle(final DeviceStatusUpdate device) {
        if (device.getRoomName().equals(BOILER)) {
            return handleBoiler(device)
                .doOnNext(boiler -> {
                    fireplace.temperature().setUpdatedAt(LocalDateTime.now());
                    fireplace.temperature().setValue(Double.parseDouble(device.getValue()));
                    logStatusUpdate(device);
                })
                .then();
        }

        return Flux.fromIterable(rooms)
            .filter(room -> room.getName().equalsIgnoreCase(device.getRoomName().name()))
            .flatMap(room -> handleRoom(room, device))
            .single()
            .doOnNext(room -> logStatusUpdate(device))
            .doOnError(throwable -> log.error(
                "Unknown Room [{}] while handling temperature sensor",
                device.getRoomName(), throwable
            ))
            .then();
    }

    private Mono<BoilerRoom> handleBoiler(final DeviceStatusUpdate device) {
        fireplace.temperature().setUpdatedAt(LocalDateTime.now());
        fireplace.temperature().setValue(Double.parseDouble(device.getValue()));
        return Mono.just(boilerRoom);
    }

    private Mono<Room> handleRoom(final Room room, final DeviceStatusUpdate device) {
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

    private void logStatusUpdate(final DeviceStatusUpdate deviceStatusUpdate) {
        log.info(
            "Status update, room: [{}], device: [{}], value: [{}]",
            deviceStatusUpdate.getRoomName().name(),
            deviceStatusUpdate.getDeviceType().name(),
            deviceStatusUpdate.getValue()
        );
    }
}
