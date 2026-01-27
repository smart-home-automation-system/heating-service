package cloud.cholewa.heating.db.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("heating_status")
public record HeatingStatusEntity(
    @Id Long id,
    LocalDateTime date,
    boolean status
) {
}
