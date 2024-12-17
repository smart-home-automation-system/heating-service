package cloud.cholewa.heating.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Alert {
    private AlertReason reason;
    private LocalDateTime createdAt;
    private LocalDateTime cancelledAt;
}
