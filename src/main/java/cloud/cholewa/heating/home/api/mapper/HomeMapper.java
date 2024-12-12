package cloud.cholewa.heating.home.api.mapper;

import cloud.cholewa.heating.home.api.model.HomeShortResponse;
import cloud.cholewa.heating.home.api.model.RoomShortResponse;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.home.model.RoomName;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HomeMapper {

    public static HomeShortResponse toHomeShortResponse(Home home) {
        return HomeShortResponse.builder()
            .boilerRoom(BoilerRoomMapper.toBoilerRoomShortResponse(home.boilerRoom()))
            .rooms(toRoomShortResponseList(home.rooms()))
            .build();
    }

    private static List<RoomShortResponse> toRoomShortResponseList(List<Room> rooms) {
        return rooms.stream()
            .filter(HomeMapper::isInternalRoom)
            .map(RoomMapper::toRoomShortResponse)
            .collect(Collectors.toList());
    }

    private static boolean isInternalRoom(final Room room) {
        return Stream.of(RoomName.LOFT, RoomName.SANCTUM, RoomName.SAUNA)
            .noneMatch(roomName -> roomName.equals(room.getName()));
    }
}
