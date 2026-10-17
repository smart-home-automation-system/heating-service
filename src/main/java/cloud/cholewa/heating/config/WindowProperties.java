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
 * The window and door sensors of the rooms (HAS-232). A bare number is minutes.
 *
 * @param holdAfter          how long a window or a door of a room has to stand open before the
 *                           heating of the room is held - a short trip through a door must not stop
 *                           it (owner, 2026-10-17: three minutes)
 * @param gatewaySilentAfter how long the state of a sensor stands without its gateway confirming it -
 *                           by publishing the sensor or by answering the read of every five minutes -
 *                           before the sensor is unknown, and after as long without a state an
 *                           unknown sensor is reported as silent. A Shelly BLU Door/Window sends only
 *                           when it changes (no beacon), so the silence of the sensor itself proves
 *                           nothing; the gateway that stopped answering does. At least two reads of
 *                           the gateway
 */
@Validated
@ConfigurationProperties(prefix = "heating.window")
public record WindowProperties(
    @NotNull @DurationMin(minutes = 1) @DurationMax(hours = 1) @DurationUnit(ChronoUnit.MINUTES)
    Duration holdAfter,
    @NotNull @DurationMin(minutes = 10) @DurationMax(hours = 24) @DurationUnit(ChronoUnit.MINUTES)
    Duration gatewaySilentAfter
) {
}
