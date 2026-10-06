package cloud.cholewa.heating.infrastructure.error;

public class BoilerException extends RuntimeException {
    public BoilerException(final String message) {
        super(message);
    }

    public BoilerException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
