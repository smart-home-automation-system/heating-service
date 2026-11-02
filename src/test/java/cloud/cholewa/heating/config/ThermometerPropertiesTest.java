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

//application.yaml as the profiles read it (HAS-368): the poll of the Gen1 thermometers carries its unit and its bounds
class ThermometerPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withInitializer(context -> context.getEnvironment().getPropertySources()
            .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withUserConfiguration(Properties.class);

    @Test
    void should_poll_the_sauna_every_minute() {
        contextRunner
            .withPropertyValues("spring.profiles.active=home")
            .run(context -> assertThat(context.getBean(ThermometerProperties.class))
                .isEqualTo(new ThermometerProperties(Duration.ofMinutes(1))));
    }

    //without the unit a bare number would bind as milliseconds: the sauna asked many times a second
    @Test
    void should_read_a_bare_number_as_seconds() {
        contextRunner
            .withPropertyValues("spring.profiles.active=home", "heating.thermometer.gen1-poll=90")
            .run(context -> assertThat(context.getBean(ThermometerProperties.class))
                .isEqualTo(new ThermometerProperties(Duration.ofSeconds(90))));
    }

    //below 30 s the pass, bounded at 25 s, would overlap its next; above an hour a heating sauna would go unseen
    @ParameterizedTest
    @ValueSource(strings = {
        "heating.thermometer.gen1-poll=29",
        "heating.thermometer.gen1-poll=PT1H1S",
        "heating.thermometer.gen1-poll="
    })
    void should_not_start_with_a_value_out_of_its_range(final String value) {
        contextRunner
            .withPropertyValues("spring.profiles.active=home", value)
            .run(context -> assertThat(context).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties(ThermometerProperties.class)
    static class Properties {
    }
}
