package cloud.cholewa.heating.home.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HomeShortResponse {
    private BoilerRoomShortResponse boilerRoom;
    private List<RoomShortResponse> rooms;
}
