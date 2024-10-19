package cloud.cholewa.heating.model;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class HeaterActor {
    private final HeaterType type;
    private boolean isHeating;
}
