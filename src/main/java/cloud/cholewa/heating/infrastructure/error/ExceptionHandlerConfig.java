package cloud.cholewa.heating.infrastructure.error;

import cloud.cholewa.commons.error.GlobalErrorExceptionHandler;
import cloud.cholewa.commons.error.processor.DomainExceptionProcessor;
import cloud.cholewa.heating.infrastructure.error.processor.HeatingExceptionProcessor;
import cloud.cholewa.heating.infrastructure.error.processor.ScheduleExceptionProcessor;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.webflux.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;

import java.util.Map;

import static cloud.cholewa.heating.infrastructure.error.HeatingErrorId.NOT_FOUND_ROOM;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Configuration
public class ExceptionHandlerConfig {

    @Bean
    @Order(-2)
    public GlobalErrorExceptionHandler globalErrorExceptionHandler(
        final ErrorAttributes errorAttributes,
        final WebProperties webProperties,
        final ApplicationContext applicationContext,
        final ServerCodecConfigurer serverCodecConfigurer
    ) {
        GlobalErrorExceptionHandler globalErrorExceptionHandler = new GlobalErrorExceptionHandler(
            errorAttributes, webProperties.getResources(), applicationContext, serverCodecConfigurer
        );

        globalErrorExceptionHandler.withCustomErrorProcessor(
            Map.ofEntries(
                Map.entry(RoomNotFoundException.class, new DomainExceptionProcessor(NOT_FOUND, NOT_FOUND_ROOM)),
                Map.entry(HeatingException.class, new HeatingExceptionProcessor()),
                Map.entry(ScheduleException.class, new ScheduleExceptionProcessor())
            )
        );

        return globalErrorExceptionHandler;
    }
}
