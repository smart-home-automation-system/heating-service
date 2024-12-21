package cloud.cholewa.heating.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Pump {

    private final PumpType name;
    private boolean running;
    private LocalDateTime startedAt;
    private LocalDateTime stoppedAt;
}
