package cloud.cholewa.heating.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Temperature {
    private double value;
    private LocalDateTime updatedAt;
}
