package cloud.cholewa.heating.home.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Data
public class RoomConfigurationResponse {

    private String name;
    private boolean isHeatingActive;
    private String temperature;
    private boolean isAnyOpeningOpened;
}
