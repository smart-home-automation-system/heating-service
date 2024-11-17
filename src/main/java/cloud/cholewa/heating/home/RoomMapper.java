package cloud.cholewa.heating.home;

import cloud.cholewa.heating.home.model.RoomConfigurationResponse;
import cloud.cholewa.heating.model.OpeningSensor;
import cloud.cholewa.heating.model.Room;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RoomMapper {

    public static RoomConfigurationResponse map(final Room room) {
        return RoomConfigurationResponse.builder()
            .name(room.getName().name())
            .isHeatingActive(room.isHeatingActive())
            .isAnyOpeningOpened(isAnyOpeningOpened(room.getOpeningSensors()))
            .temperature(room.getTemperatureSensor())
            .build();
    }

    private static boolean isAnyOpeningOpened(final List<OpeningSensor> sensors) {
        return sensors != null && sensors.stream()
            .anyMatch(OpeningSensor::isOpen);
    }

//    private static TemperatureSensor getTemperature(final TemperatureSensor sensor) {
//        return TemperatureSensor.builder()
//            .temperature(sensor.getUpdateTime() != null
//                ? String.valueOf(sensor.getTemperature())
//                : "unknown")
//            .build();
//
//
//        return ;
//    }
}
