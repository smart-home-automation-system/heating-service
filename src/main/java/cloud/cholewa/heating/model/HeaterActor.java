package cloud.cholewa.heating.model;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class HeaterActor {
    private final HeaterType name;
    private boolean working;
}
