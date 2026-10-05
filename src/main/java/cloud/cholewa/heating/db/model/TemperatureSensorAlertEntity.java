package cloud.cholewa.heating.db.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("temperature_sensor_alert")
public record TemperatureSensorAlertEntity(
    @Id Long id,
    String room,
    LocalDateTime staleSince,
    LocalDateTime lastAlertAt
) {
}
