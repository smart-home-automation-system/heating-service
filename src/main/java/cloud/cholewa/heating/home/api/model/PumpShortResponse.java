package cloud.cholewa.heating.home.api.model;

import cloud.cholewa.heating.model.PumpType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PumpShortResponse {
    private PumpType name;
    private boolean running;
}
