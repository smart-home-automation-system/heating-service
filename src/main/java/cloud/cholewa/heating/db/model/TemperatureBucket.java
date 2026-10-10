package cloud.cholewa.heating.db.model;

import java.time.LocalDateTime;

/**
 * The readings of one room within one bucket of time, as the history query answers them:
 * {@code at} is the start of the bucket, {@code temperature} the average, rounded to 2 decimals.
 */
public record TemperatureBucket(
    LocalDateTime at,
    Double temperature
) {
}
