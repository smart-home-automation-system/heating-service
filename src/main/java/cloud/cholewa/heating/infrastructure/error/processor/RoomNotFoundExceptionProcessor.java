package cloud.cholewa.heating.infrastructure.error.processor;

import cloud.cholewa.commons.error.model.ErrorId;
import cloud.cholewa.commons.error.model.ErrorMessage;
import cloud.cholewa.commons.error.model.Errors;
import cloud.cholewa.commons.error.processor.ExceptionProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.util.Collections;

import static cloud.cholewa.heating.infrastructure.error.HeatingErrorId.NOT_FOUND_ROOM;

@Slf4j
public class RoomNotFoundExceptionProcessor implements ExceptionProcessor {

    @Override
    public Errors apply(final Throwable throwable) {
        //the caller's mistake: at ERROR it would feed the "Error log spike" rule for nothing
        log.warn("Handled [{}]: {}", throwable.getClass().getSimpleName(), throwable.getMessage());

        return Errors.builder()
            .httpStatus(HttpStatus.NOT_FOUND)
            .errors(Collections.singleton(
                ErrorMessage.builder()
                    .message(NOT_FOUND_ROOM.getDescription())
                    .details("Room name: " + throwable.getMessage())
                    .code(ErrorId.codeOf(NOT_FOUND_ROOM))
                    .build()
            ))
            .build();
    }
}
