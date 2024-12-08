package cloud.cholewa.heating.infrastructure.error;

public class DeviceException extends RuntimeException {
    public DeviceException(final String message) {
        super(message);
    }
}
