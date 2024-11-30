package cloud.cholewa.heating.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HeaterActor {
    private final HeaterType name;
    private boolean working;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
