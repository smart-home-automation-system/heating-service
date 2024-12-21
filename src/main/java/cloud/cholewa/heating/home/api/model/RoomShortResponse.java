package cloud.cholewa.heating.home.api.model;

import cloud.cholewa.heating.model.Temperature;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RoomShortResponse {
    private String name;
    private boolean heatingActive;
    private Temperature temperature;
    private boolean opened;
}
