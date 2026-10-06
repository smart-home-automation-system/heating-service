package cloud.cholewa.heating.config;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * {@code connectTimeout} - how long a Shelly relay may take to accept the connection.<br>
 * {@code responseTimeout} - the longest silence while its response is read.<br>
 * A bare number is seconds: without the unit it would bind as milliseconds and every call would
 * time out. Zero would switch the timeout off, which is the unbounded wait these exist to end,
 * and so would a value meant as milliseconds ("5000") - hence the upper bound.
 */
@Validated
@ConfigurationProperties(prefix = "shelly.actor")
public record ShellyTimeoutProperties(
    @NotNull @DurationMin(seconds = 1) @DurationMax(seconds = 60) @DurationUnit(ChronoUnit.SECONDS)
    @DefaultValue("10s") Duration connectTimeout,
    @NotNull @DurationMin(seconds = 1) @DurationMax(seconds = 60) @DurationUnit(ChronoUnit.SECONDS)
    @DefaultValue("5s") Duration responseTimeout
) {
}
