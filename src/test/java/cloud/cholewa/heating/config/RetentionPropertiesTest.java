package cloud.cholewa.heating.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

//HAS-413: the retention of the room temperatures and the changes of the house switch
class RetentionPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withInitializer(context -> context.getEnvironment().getPropertySources()
            .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withUserConfiguration(Properties.class);

    @Test
    void should_keep_730_days() {
        contextRunner
            .withPropertyValues("spring.profiles.active=home")
            .run(context -> assertThat(context.getBean(RetentionProperties.class))
                .isEqualTo(new RetentionProperties(Duration.ofDays(730))));
    }

    //without the unit a bare number would bind as milliseconds: every reading older than a second deleted
    @Test
    void should_read_a_bare_number_as_days() {
        contextRunner
            .withPropertyValues("spring.profiles.active=home", "heating.retention=400")
            .run(context -> assertThat(context.getBean(RetentionProperties.class))
                .isEqualTo(new RetentionProperties(Duration.ofDays(400))));
    }

    //below a year the comparison with the year before is gone; above ten years it is no retention
    @ParameterizedTest
    @ValueSource(strings = {
        "heating.retention=365",
        "heating.retention=3651",
        "heating.retention="
    })
    void should_not_start_with_a_value_out_of_its_range(final String value) {
        contextRunner
            .withPropertyValues("spring.profiles.active=home", value)
            .run(context -> assertThat(context).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties(RetentionProperties.class)
    static class Properties {
    }
}
