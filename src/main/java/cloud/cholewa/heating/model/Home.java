package cloud.cholewa.heating.model;

import lombok.Data;

import java.util.List;

@Data
public class Home {

    private final BoilerRoom boiler;
    private final List<Room> rooms;
    private boolean isHeatingAllowed = false;
}
