package cloud.cholewa.heating.db.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("opening_sensor_alert")
public record OpeningSensorAlertEntity(
    @Id Long id,
    Long sensorId,
    LocalDateTime silentSince,
    LocalDateTime lastAlertAt
) {
}
