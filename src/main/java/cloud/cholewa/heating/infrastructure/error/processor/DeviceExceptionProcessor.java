package cloud.cholewa.heating.infrastructure.error.processor;

import cloud.cholewa.commons.error.model.ErrorMessage;
import cloud.cholewa.commons.error.model.Errors;
import cloud.cholewa.commons.error.processor.ExceptionProcessor;
import org.springframework.http.HttpStatus;

import java.util.Collections;

public class DeviceExceptionProcessor implements ExceptionProcessor {

    @Override
    public Errors apply(final Throwable throwable) {
        return Errors.builder()
            .httpStatus(HttpStatus.BAD_REQUEST)
            .errors(Collections.singleton(
                ErrorMessage.builder()
                    .message("Unknown device type")
                    .details(throwable.getMessage())
                    .build()
            ))
            .build();
    }
}
