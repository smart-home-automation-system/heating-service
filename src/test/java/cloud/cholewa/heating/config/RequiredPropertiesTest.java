package cloud.cholewa.heating.config;

import cloud.cholewa.commons.startup.RequiredPropertiesAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

//The three passwords of the broker have no default in the shared document, and the list of
//cholewa.required-properties makes a pod without one of them refuse to start (cholewa-commons; it
//replaced the own BrokerPasswordCheck in HAS-232). This ties the list to the placeholders of
//application.yaml, profile by profile. The active profiles are given here, above the system property
//surefire sets for every test class
class RequiredPropertiesTest {

    //without the variables of the machine: a developer who exported a password for a local run would
    //otherwise see the tests of a missing one fail
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withInitializer(context -> context.getEnvironment().getPropertySources()
            .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withConfiguration(AutoConfigurations.of(RequiredPropertiesAutoConfiguration.class));

    @Test
    void should_not_start_in_the_cluster_without_the_passwords() {
        contextRunner
            .withPropertyValues("spring.profiles.active=home")
            .run(context -> assertThat(context.getStartupFailure()).hasStackTraceContaining(
                "A required property has no value: spring.rabbitmq.password (set rabbitmq-password), "
                    + "notification.password (set notification-rabbitmq-password), "
                    + "device.password (set device-rabbitmq-password)"));
    }

    @ParameterizedTest
    @CsvSource({
        "notification-rabbitmq-password, device-rabbitmq-password, spring.rabbitmq.password (set rabbitmq-password)",
        "rabbitmq-password, device-rabbitmq-password, notification.password (set notification-rabbitmq-password)",
        "rabbitmq-password, notification-rabbitmq-password, device.password (set device-rabbitmq-password)"
    })
    void should_not_start_in_the_cluster_without_one_of_the_passwords(
        final String given, final String alsoGiven, final String missing) {
        contextRunner
            .withPropertyValues("spring.profiles.active=home", given + "=secret", alsoGiven + "=secret")
            .run(context -> assertThat(context.getStartupFailure())
                .hasMessage("A required property has no value: " + missing));
    }

    @Test
    void should_start_in_the_cluster_with_the_values_of_the_deployment() {
        contextRunner
            .withPropertyValues(
                "spring.profiles.active=home",
                "rabbitmq-password=listenerSecret",
                "notification-rabbitmq-password=publisherSecret",
                "device-rabbitmq-password=deviceSecret"
            )
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getEnvironment().getProperty("spring.rabbitmq.password")).isEqualTo("listenerSecret");
                assertThat(context.getEnvironment().getProperty("notification.password")).isEqualTo("publisherSecret");
                assertThat(context.getEnvironment().getProperty("device.password")).isEqualTo("deviceSecret");
            });
    }

    //a run on a workstation neither consumes nor publishes unless it is given the real passwords
    @Test
    void should_start_on_a_workstation_without_the_passwords() {
        contextRunner
            .withPropertyValues("spring.profiles.active=home,local")
            .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void should_start_under_the_test_profile() {
        contextRunner
            .withPropertyValues("spring.profiles.active=test")
            .run(context -> assertThat(context).hasNotFailed());
    }
}
