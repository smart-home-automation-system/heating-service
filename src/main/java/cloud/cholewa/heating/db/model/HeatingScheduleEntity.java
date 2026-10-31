package cloud.cholewa.heating.db.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

//A row of heating_schedule (V5, HAS-355) as it is stored: the names of the room and the heater and the
//days are text, read into the model by HeatingScheduleService - a row this version cannot read is skipped
//there, not refused by the mapping
@Table("heating_schedule")
public record HeatingScheduleEntity(
    @Id Long id,
    String room,
    String heater,
    List<String> days,
    LocalTime startTime,
    LocalTime endTime,
    BigDecimal temperature,
    boolean enabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
