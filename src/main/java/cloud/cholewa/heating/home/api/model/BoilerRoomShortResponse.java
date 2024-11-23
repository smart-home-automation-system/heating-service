package cloud.cholewa.heating.home.api.model;

import cloud.cholewa.heating.model.Fireplace;
import cloud.cholewa.heating.model.Furnace;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class BoilerRoomShortResponse {
    private boolean heatingEnabled;
    private final Furnace furnace;
    private final Fireplace fireplace;
    private final List<PumpShortResponse> pumps;
}
