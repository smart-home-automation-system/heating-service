package cloud.cholewa.heating.db.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDateTime;

//the last reading of a window or door sensor (HAS-419, V7): updatedAt the time of its gateway, the others house times
@Table("window_sensor_reading")
public record WindowSensorReadingEntity(
    @Id Long sensorId,
    Boolean open,
    Instant updatedAt,
    LocalDateTime openSince,
    LocalDateTime storedAt
) {
}
