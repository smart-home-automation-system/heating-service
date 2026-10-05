package cloud.cholewa.heating.config;

import cloud.cholewa.home.model.RoomName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * {@code staleAfter} - how long a temperature sensor may stay silent before it is reported.<br>
 * {@code reminderInterval} - how often the alert is repeated while the sensor stays silent.<br>
 * {@code mutedRooms} - rooms whose sensor is not reported, e.g. one taken out of service.<br>
 * A bare number is hours: without the unit it would bind as milliseconds and every sensor would be
 * reported as silent on each check.
 */
@Validated
@ConfigurationProperties(prefix = "heating.sensor-monitor")
public record SensorMonitorProperties(
    @NotBlank String cron,
    @NotNull @DurationMin(hours = 1) @DurationUnit(ChronoUnit.HOURS) @DefaultValue("24h") Duration staleAfter,
    @NotNull @DurationMin(hours = 1) @DurationUnit(ChronoUnit.HOURS) @DefaultValue("24h") Duration reminderInterval,
    @NotNull @DefaultValue Set<RoomName> mutedRooms
) {
}
