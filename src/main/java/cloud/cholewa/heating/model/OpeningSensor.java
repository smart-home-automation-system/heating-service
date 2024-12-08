package cloud.cholewa.heating.model;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class OpeningSensor {
    private final OpeningType name;
    private boolean open;
}
