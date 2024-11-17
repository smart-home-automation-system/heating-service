package cloud.cholewa.heating.water.db.model;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("hot_water_temperature")
@Value
@Builder
public class HotWaterTemperatureEntity {

    @Id
    Long id;

    @NotNull
    LocalDateTime timestamp;

    @NotNull
    Double waterTemperature;

    @NotNull
    Double circulationTemperature;
}
