package cloud.cholewa.heating.home.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Getter
public class RoomConfigurationResponse {

    private String name;
    private boolean isHeatingActive;
    private String temperature;
    private boolean isAnyOpeningOpened;
}
