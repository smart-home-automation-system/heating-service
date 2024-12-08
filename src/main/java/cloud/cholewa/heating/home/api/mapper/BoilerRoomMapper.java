package cloud.cholewa.heating.home.api.mapper;

import cloud.cholewa.heating.home.api.model.BoilerRoomShortResponse;
import cloud.cholewa.heating.home.api.model.PumpShortResponse;
import cloud.cholewa.heating.model.BoilerRoom;
import cloud.cholewa.heating.model.Pump;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BoilerRoomMapper {

    public static BoilerRoomShortResponse toBoilerRoomShortResponse(BoilerRoom boilerRoom) {
        return BoilerRoomShortResponse.builder()
            .heatingEnabled(boilerRoom.isHeatingEnabled())
            .furnace(boilerRoom.getFurnace())
            .fireplace(boilerRoom.getFireplace())
            .pumps(toPumpShortResponses(boilerRoom.getPumps()))
            .build();
    }

    private static List<PumpShortResponse> toPumpShortResponses(List<Pump> pumps) {
        return pumps.stream()
            .map(PumpMapper::toPumpShortResponse)
            .collect(Collectors.toList());
    }
}
