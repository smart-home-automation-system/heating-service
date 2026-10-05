package cloud.cholewa.heating.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Where the notifications are published. Host and port are those of {@code spring.rabbitmq.*} -
 * it is the same broker, only another virtual host.<br>
 * {@code env} - the value of the {@code env} header the exchange routes by: {@code prod} or {@code dev}.
 */
@Validated
@ConfigurationProperties(prefix = "notification")
public record NotificationProperties(
    @NotBlank String virtualHost,
    @NotBlank String username,
    @NotBlank String password,
    @NotBlank String exchange,
    @NotBlank String env
) {
}
