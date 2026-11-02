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
 * The Shelly thermometers that can only be polled (HAS-368). A bare number is seconds.
 *
 * @param gen1Poll how often a Gen1 thermometer - the Shelly 1 with the temperature add-on of the sauna - is read
 *                 (PT1M). At least 30 s - the pass is bounded below that - at most an hour, so a sauna that heats up
 *                 is seen while it does
 */
@Validated
@ConfigurationProperties(prefix = "heating.thermometer")
public record ThermometerProperties(
    @NotNull @DurationMin(seconds = 30) @DurationMax(hours = 1) @DurationUnit(ChronoUnit.SECONDS) Duration gen1Poll
) {
}
