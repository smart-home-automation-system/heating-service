package cloud.cholewa.heating.model;

import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
public class BoilerRoom {

    private final List<HeatingSource> heatingSources;
    private final List<Pump> pumps;
}
