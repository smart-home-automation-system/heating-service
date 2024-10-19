package cloud.cholewa.heating.home;

import cloud.cholewa.heating.home.model.RoomConfigurationResponse;
import cloud.cholewa.heating.model.OpeningSensor;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.TemperatureSensor;
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
            .temperature(getTemperature(room.getTemperatureSensor()))
            .build();
    }

    private static boolean isAnyOpeningOpened(List<OpeningSensor> sensors) {
        return sensors.stream()
            .anyMatch(OpeningSensor::isOpen);
    }

    private static String getTemperature(TemperatureSensor sensor) {
        return sensor.getUpdateTime() != null
            ? String.valueOf(sensor.getTemperature())
            : "unknown";
    }
}
