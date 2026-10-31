package cloud.cholewa.heating.infrastructure.error.processor;

import cloud.cholewa.commons.error.model.ErrorId;
import cloud.cholewa.commons.error.model.ErrorMessage;
import cloud.cholewa.commons.error.model.Errors;
import cloud.cholewa.commons.error.processor.ExceptionProcessor;
import cloud.cholewa.heating.infrastructure.error.ScheduleException;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

//A refused write of a schedule is the caller's mistake: a WARN, not an ERROR the alerts would count, and
//the reason is answered in the details for the dashboard to show
@Slf4j
public class ScheduleExceptionProcessor implements ExceptionProcessor {

    @Override
    public Errors apply(final Throwable throwable) {
        final ScheduleException exception = (ScheduleException) throwable;
        final String code = ErrorId.codeOf(exception.getError());

        log.warn("Heating schedule refused ({}, {}): {}", exception.getHttpStatus().value(), code, exception.getMessage());

        return Errors.builder()
            .httpStatus(exception.getHttpStatus())
            .errors(Collections.singleton(
                ErrorMessage.builder()
                    .message(exception.getError().getDescription())
                    .details(exception.getMessage())
                    .code(code)
                    .build()
            ))
            .build();
    }
}
