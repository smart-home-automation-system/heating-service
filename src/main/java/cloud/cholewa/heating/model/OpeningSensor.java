package cloud.cholewa.heating.model;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class OpeningSensor {

    private final String name;
    private final OpeningType openingType;
    private boolean isOpen;
}
