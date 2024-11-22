package cloud.cholewa.heating.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
public class BoilerRoom {

    private boolean heatingEnabled;
    private final Furnace furnace;
    private final Fireplace fireplace;
    private final HotWater hotWater;
    private final List<Pump> pumps;
}
