package cloud.cholewa.heating.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ShellyTimeoutPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
        .withUserConfiguration(TestConfig.class);

    @Test
    void should_default_to_ten_seconds_to_connect_and_five_for_the_response() {
        contextRunner.run(context -> {
            final ShellyTimeoutProperties properties = context.getBean(ShellyTimeoutProperties.class);

            assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(10));
            assertThat(properties.responseTimeout()).isEqualTo(Duration.ofSeconds(5));
        });
    }

    //without @DurationUnit a bare number is milliseconds: "5" would time every call out
    @Test
    void should_read_bare_number_as_seconds() {
        contextRunner
            .withPropertyValues("shelly.actor.connect-timeout=3", "shelly.actor.response-timeout=7")
            .run(context -> {
                final ShellyTimeoutProperties properties = context.getBean(ShellyTimeoutProperties.class);

                assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
                assertThat(properties.responseTimeout()).isEqualTo(Duration.ofSeconds(7));
            });
    }

    //zero switches netty's timeout off - the unbounded wait the timeouts exist to end
    @Test
    void should_refuse_response_timeout_of_zero() {
        contextRunner
            .withPropertyValues("shelly.actor.response-timeout=0")
            .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void should_refuse_response_timeout_shorter_than_one_second() {
        contextRunner
            .withPropertyValues("shelly.actor.response-timeout=500ms")
            .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void should_refuse_connect_timeout_shorter_than_one_second() {
        contextRunner
            .withPropertyValues("shelly.actor.connect-timeout=500ms")
            .run(context -> assertThat(context).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties(ShellyTimeoutProperties.class)
    static class TestConfig {
    }
}
