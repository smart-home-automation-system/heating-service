package cloud.cholewa.heating.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Represents the current status of the home heating system.
 * {@code enabledHomeHeatingSystem} - indicates if heating is allowed in the home, the last value persisted in the database.
 * {@code homeHeatingSystemUpdatedAt} - represents the last time when enabledHomeHeatingSystem was updated.
 * {@code isHomeHeatingEnabled} - indicates if any of the heaters are enabled in the home and this value is returned to boiler-service
 */
@Data
public class HomeStatus {

    private boolean enabledHomeHeatingSystem;
    private LocalDateTime homeHeatingSystemUpdatedAt;

    private boolean anyHeaterActive;
}
