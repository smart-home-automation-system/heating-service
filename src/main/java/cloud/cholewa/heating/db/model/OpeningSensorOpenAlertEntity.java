package cloud.cholewa.heating.db.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

//a sensor told to the household as possibly stuck open (HAS-350, V6), house times
@Table("opening_sensor_open_alert")
public record OpeningSensorOpenAlertEntity(
    @Id Long id,
    Long sensorId,
    LocalDateTime openSince,
    LocalDateTime stateUpdatedAt,
    LocalDateTime alertedAt
) {
}
