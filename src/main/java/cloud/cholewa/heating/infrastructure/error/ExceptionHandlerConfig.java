package cloud.cholewa.heating.infrastructure.error;

import cloud.cholewa.commons.error.GlobalErrorExceptionHandler;
import cloud.cholewa.heating.infrastructure.error.processor.HeatingExceptionProcessor;
import cloud.cholewa.heating.infrastructure.error.processor.RoomNotFoundExceptionProcessor;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.webflux.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;

import java.util.Map;

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
                Map.entry(RoomNotFoundException.class, new RoomNotFoundExceptionProcessor()),
                Map.entry(HeatingException.class, new HeatingExceptionProcessor())
            )
        );

        return globalErrorExceptionHandler;
    }
}
