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

//application.yaml as the profiles read it (HAS-232): a duration that decides when a room stops
//heating carries its unit and its bounds
class WindowPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withInitializer(context -> context.getEnvironment().getPropertySources()
            .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withUserConfiguration(Properties.class);

    //owner, 2026-10-17: three minutes, not five
    @Test
    void should_hold_the_heating_after_three_minutes_and_keep_a_state_half_an_hour() {
        contextRunner
            .withPropertyValues("spring.profiles.active=home")
            .run(context -> assertThat(context.getBean(WindowProperties.class))
                .isEqualTo(new WindowProperties(Duration.ofMinutes(3), Duration.ofMinutes(30))));
    }

    //without the unit a bare number would bind as milliseconds: every open door would stop the heating
    @Test
    void should_read_a_bare_number_as_minutes() {
        contextRunner
            .withPropertyValues("spring.profiles.active=home", "heating.window.hold-after=4",
                "heating.window.gateway-silent-after=45")
            .run(context -> assertThat(context.getBean(WindowProperties.class))
                .isEqualTo(new WindowProperties(Duration.ofMinutes(4), Duration.ofMinutes(45))));
    }

    //a hold below a minute would stop the heating for every trip through a door; above an hour it
    //would never hold. Less than two reads of the gateways would make a window unknown after one
    //missed read; more than a day would keep a dead gateway's windows for ever
    @ParameterizedTest
    @ValueSource(strings = {
        "heating.window.hold-after=0",
        "heating.window.hold-after=PT59S",
        "heating.window.hold-after=PT1H1M",
        "heating.window.hold-after=",
        "heating.window.gateway-silent-after=9",
        "heating.window.gateway-silent-after=PT24H1M",
        "heating.window.gateway-silent-after="
    })
    void should_not_start_with_a_value_out_of_its_range(final String value) {
        contextRunner
            .withPropertyValues("spring.profiles.active=home", value)
            .run(context -> assertThat(context).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties(WindowProperties.class)
    static class Properties {
    }
}
