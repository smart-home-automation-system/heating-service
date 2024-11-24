package cloud.cholewa.heating.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Furnace {
    private boolean running;
    private LocalDateTime startedAt;
    private LocalDateTime stoppedAt;
}
