package cloud.cholewa.heating.db.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("room_temperature")
public record TemperatureEntity(
    @Id Long id,
    LocalDateTime date,
    String room,
    Double temperature
) {

}
