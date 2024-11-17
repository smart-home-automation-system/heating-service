package cloud.cholewa.heating.home.model;

import cloud.cholewa.heating.model.BoilerRoom;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;

@NoArgsConstructor
@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@SuperBuilder
public class HomeConfigurationResponse {

    private boolean isHeatingAllowed;
    private BoilerRoom boilerRoom;
    private int roomNumber;
    private List<RoomConfigurationResponse> rooms;
}
