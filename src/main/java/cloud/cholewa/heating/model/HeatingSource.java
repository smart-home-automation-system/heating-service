package cloud.cholewa.heating.model;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class HeatingSource {

    private final HeatingSourceType type;
    private boolean isActive;
}
