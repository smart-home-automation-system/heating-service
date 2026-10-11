package cloud.cholewa.heating.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.List;

//The two passwords of the broker come from the environment and have no default, so that a pod
//without them does not start. Left to Spring Boot it would: the binder keeps a placeholder it
//cannot resolve as its text, the password becomes the string "${notification-rabbitmq-password}",
//passes @NotBlank, the pod turns Ready, and the broker refuses the first notification - which is
//sent when a sensor is already silent (HAS-217). The environment itself is strict about the same
//placeholder, so asking it is the whole check
@Configuration(proxyBeanMethods = false)
public class BrokerPasswordCheck {

    //the listener on /temperature, and the second connection the notifications are published over
    static final List<String> PASSWORDS = List.of("spring.rabbitmq.password", "notification.password");

    public BrokerPasswordCheck(final Environment environment) {
        PASSWORDS.forEach(property -> require(environment, property));
    }

    private static void require(final Environment environment, final String property) {
        final String value;
        try {
            value = environment.getProperty(property);
        } catch (IllegalArgumentException e) {
            //Spring's message names the variable: Could not resolve placeholder 'rabbitmq-password' ...
            throw new IllegalStateException(
                "The password of the broker is not set (" + property + "): " + e.getMessage(), e);
        }
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("The password of the broker is empty (" + property + ")");
        }
    }
}
