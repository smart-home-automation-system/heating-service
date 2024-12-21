package cloud.cholewa.heating.home.api.mapper;

import cloud.cholewa.heating.home.api.model.PumpShortResponse;
import cloud.cholewa.heating.model.Pump;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PumpMapper {

    public static PumpShortResponse toPumpShortResponse(Pump pump) {
        return PumpShortResponse.builder()
            .name(pump.getName())
            .running(pump.isRunning())
            .build();
    }
}
