package cloud.cholewa.heating.infrastructure.error.processor;

import cloud.cholewa.commons.error.model.ErrorMessage;
import cloud.cholewa.commons.error.model.Errors;
import cloud.cholewa.commons.error.processor.ExceptionProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.util.Collections;

@Slf4j
public class HotWaterExceptionProcessor implements ExceptionProcessor {

    @Override
    public Errors apply(final Throwable throwable) {
        log.error("Hot Water sensor problem");

        return Errors.builder()
            .httpStatus(HttpStatus.SERVICE_UNAVAILABLE)
            .errors(Collections.singleton(
                ErrorMessage.builder()
                    .message("Hot Water sensor problem")
                    .details(throwable.getMessage())
                    .build()
            ))
            .build();
    }
}
