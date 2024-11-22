package cloud.cholewa.heating.model;

import java.util.List;

public record Home(BoilerRoom boilerRoom, List<Room> rooms) {
}
