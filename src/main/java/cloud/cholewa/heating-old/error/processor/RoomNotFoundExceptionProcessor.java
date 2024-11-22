//package cloud.cholewa.heating.error.processor;
//
//import cloud.cholewa.commons.error.model.ErrorMessage;
//import cloud.cholewa.commons.error.model.Errors;
//import cloud.cholewa.commons.error.processor.ExceptionProcessor;
//import cloud.cholewa.heating.error.CustomError;
//import cloud.cholewa.heating.error.RoomNotFoundException;
//import org.springframework.http.HttpStatus;
//
//import java.util.Collections;
//
//public class RoomNotFoundExceptionProcessor implements ExceptionProcessor {
//
//    @Override
//    public Errors apply(final Throwable throwable) {
//        RoomNotFoundException roomNotFoundException = (RoomNotFoundException) throwable;
//
//        return Errors.builder()
//            .httpStatus(HttpStatus.NOT_FOUND)
//            .errors(Collections.singleton(
//                ErrorMessage.builder()
//                    .message(CustomError.ROOM_NOT_FOUND.getDescription())
//                    .details("Room name: " + roomNotFoundException.getLocalizedMessage())
//                    .build()
//            ))
//            .build();
//    }
//}
