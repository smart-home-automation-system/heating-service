package cloud.cholewa.heating.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The third connection to the broker, the one the events of the devices are consumed over
 * (HAS-232): every MQTT client of the house publishes to {@code amq.topic} of {@code /device}, and
 * the queue of this service there is bound with {@code #}. Host and port are those of
 * {@code spring.rabbitmq.*} - it is the same broker, only another virtual host.<br>
 * {@code queue} - {@code device.<env>.heating}.
 */
@Validated
@ConfigurationProperties(prefix = "device")
public record DeviceConnectionProperties(
    @NotBlank String virtualHost,
    @NotBlank String username,
    @NotBlank String password,
    @NotBlank String queue
) {

    //a record prints its components, and one of them is the password
    @Override
    public String toString() {
        return "DeviceConnectionProperties[virtualHost=" + virtualHost + ", username=" + username
            + ", queue=" + queue + "]";
    }
}
