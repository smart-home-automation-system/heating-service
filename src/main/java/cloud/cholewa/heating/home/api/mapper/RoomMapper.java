package cloud.cholewa.heating.home.api.mapper;

import cloud.cholewa.heating.home.api.model.RoomShortResponse;
import cloud.cholewa.heating.model.Room;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RoomMapper {

    public static RoomShortResponse toRoomShortResponse(Room room) {
        return RoomShortResponse.builder()
            .name(room.getName().name())
            .temperature(room.getTemperature())
            .build();
    }
}
