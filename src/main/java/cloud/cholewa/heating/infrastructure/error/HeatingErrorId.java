package cloud.cholewa.heating.infrastructure.error;

import cloud.cholewa.commons.error.model.ErrorId;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

//The name of a constant is the code of the error response (ErrorMessage.code), so it is wire
//contract: a caller tells "no such room" from a 404 of the routing by it. Renaming one compiles
//and passes everything else here - HeatingErrorIdTest pins the names. The description is the
//message of the response, worded for people, and may be reworded.
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum HeatingErrorId implements ErrorId {

    NOT_FOUND_ROOM("Room with provided name is not a part of home"),
    //the room has no such heater, or the service does not drive it (SANCTUM's radiator) - HAS-356
    NOT_FOUND_HEATER("Heater not found"),
    NOT_FOUND_SCHEDULE("Schedule not found"),
    //a schedule that breaks a rule of ScheduleRules; the details say which
    INVALID_SCHEDULE("Invalid schedule");

    @Getter
    private final String description;
}
