package cloud.cholewa.heating.config;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * How long the room temperatures and the changes of the house switch are kept (HAS-413). A bare number is days.
 *
 * @param retention the age above which a row is deleted every night (730d, as water, weather and energy keep). At
 *                  least a year - the dashboard compares a year with the one before - at most ten years
 */
@Validated
@ConfigurationProperties(prefix = "heating")
public record RetentionProperties(
    @NotNull @DurationMin(days = 366) @DurationMax(days = 3650) @DurationUnit(ChronoUnit.DAYS) Duration retention
) {
}
