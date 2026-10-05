package cloud.cholewa.heating.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SensorMonitorPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
        .withUserConfiguration(TestConfig.class)
        .withPropertyValues("heating.sensor-monitor.cron=0 0 * * * *");

    @Test
    void should_default_to_one_day() {
        contextRunner.run(context -> {
            final SensorMonitorProperties properties = context.getBean(SensorMonitorProperties.class);

            assertThat(properties.staleAfter()).isEqualTo(Duration.ofHours(24));
            assertThat(properties.reminderInterval()).isEqualTo(Duration.ofHours(24));
        });
    }

    //without @DurationUnit a bare number is milliseconds: "24" would report every sensor on each pass
    @Test
    void should_read_bare_number_as_hours() {
        contextRunner
            .withPropertyValues(
                "heating.sensor-monitor.stale-after=36",
                "heating.sensor-monitor.reminder-interval=12"
            )
            .run(context -> {
                final SensorMonitorProperties properties = context.getBean(SensorMonitorProperties.class);

                assertThat(properties.staleAfter()).isEqualTo(Duration.ofHours(36));
                assertThat(properties.reminderInterval()).isEqualTo(Duration.ofHours(12));
            });
    }

    @Test
    void should_refuse_limit_shorter_than_one_hour() {
        contextRunner
            .withPropertyValues("heating.sensor-monitor.stale-after=30m")
            .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void should_refuse_reminder_interval_shorter_than_one_hour() {
        contextRunner
            .withPropertyValues("heating.sensor-monitor.reminder-interval=59m")
            .run(context -> assertThat(context).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties(SensorMonitorProperties.class)
    static class TestConfig {
    }
}
