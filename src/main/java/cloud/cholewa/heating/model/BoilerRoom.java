package cloud.cholewa.heating.model;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
public class BoilerRoom {

    private final List<HeatingSource> heatingSources;
    private final HotWater hotWater;
    private final List<Pump> pumps;
}
