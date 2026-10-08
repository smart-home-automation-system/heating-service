package cloud.cholewa.heating.model;

import com.fasterxml.jackson.annotation.JsonValue;

//the value is the name of the type in the replies of the API (RoomReply), so it is contract
public enum HeaterType {
    RADIATOR("radiator"),
    FLOOR("floor");

    private final String value;

    HeaterType(final String value) {
        this.value = value;
    }

    @JsonValue
    @Override
    public String toString() {
        return value;
    }
}
