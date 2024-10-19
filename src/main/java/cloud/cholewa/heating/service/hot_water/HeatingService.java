package cloud.cholewa.heating.service.hot_water;

import cloud.cholewa.heating.model.Home;
import org.springframework.stereotype.Service;

import static cloud.cholewa.heating.config.home.HomeConfig.getBoilerRoomConfiguration;
import static cloud.cholewa.heating.config.home.HomeConfig.getRoomsConfiguration;

@Service
public class HeatingService {

    private final Home home;

    public HeatingService() {
        this.home = new Home(getRoomsConfiguration(), getBoilerRoomConfiguration());
    }
}
