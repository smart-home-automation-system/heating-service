package cloud.cholewa.heating.infrastructure.error;

public class HotWaterException extends RuntimeException {
    public HotWaterException(final String message) {
        super(message);
    }
}
