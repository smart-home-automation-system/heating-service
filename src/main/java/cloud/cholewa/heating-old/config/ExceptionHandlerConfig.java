//package cloud.cholewa.heating.config;
//
//import cloud.cholewa.commons.error.GlobalErrorExceptionHandler;
//import cloud.cholewa.heating.error.RoomNotFoundException;
//import cloud.cholewa.heating.error.processor.RoomNotFoundExceptionProcessor;
//import org.springframework.boot.autoconfigure.web.WebProperties;
//import org.springframework.boot.web.reactive.error.ErrorAttributes;
//import org.springframework.context.ApplicationContext;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.annotation.Order;
//import org.springframework.http.codec.ServerCodecConfigurer;
//
//import java.util.Map;
//
//@Configuration
//public class ExceptionHandlerConfig {
//
//    @Bean
//    @Order(-2)
//    public GlobalErrorExceptionHandler globalErrorExceptionHandler(
//        final ErrorAttributes errorAttributes,
//        final WebProperties webProperties,
//        final ApplicationContext applicationContext,
//        final ServerCodecConfigurer serverCodecConfigurer
//    ) {
//        GlobalErrorExceptionHandler globalErrorExceptionHandler = new GlobalErrorExceptionHandler(
//            errorAttributes, webProperties.getResources(), applicationContext, serverCodecConfigurer
//        );
//
//        globalErrorExceptionHandler.withCustomErrorProcessor(
//            Map.ofEntries(
//                Map.entry(RoomNotFoundException.class, new RoomNotFoundExceptionProcessor())
//            )
//        );
//
//        return globalErrorExceptionHandler;
//    }
//}
