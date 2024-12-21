package cloud.cholewa.heating.infrastructure.error;

public class HeatingException extends RuntimeException {
    public HeatingException(final String message) {
        super(message);
    }
}
