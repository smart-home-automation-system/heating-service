package cloud.cholewa.heating.model;

import lombok.Data;

import java.util.List;

@Data
public class Home {

    private final List<Room> rooms;
    private final BoilerRoom boiler;
    private boolean isHeatingAllowed = false;
}
