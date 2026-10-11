package cloud.cholewa.heating.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

//application.yaml as the profiles read it, with the properties of the two connections and nothing
//that could open one. The active profiles are given here, above the system property surefire sets
//for every test class
class BrokerPasswordCheckTest {

    private final ApplicationContextRunner withoutTheCheck = new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withUserConfiguration(BrokerProperties.class);

    private final ApplicationContextRunner contextRunner = withoutTheCheck
        .withUserConfiguration(BrokerPasswordCheck.class);

    //why the check exists: nothing in Spring Boot minds the missing variables, @NotBlank included
    @Test
    void should_take_the_placeholders_for_the_passwords_when_nothing_checks() {
        withoutTheCheck
            .withPropertyValues("spring.profiles.active=home")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(RabbitProperties.class).getPassword())
                    .isEqualTo("${rabbitmq-password}");
                assertThat(context.getBean(NotificationConnectionProperties.class).password())
                    .isEqualTo("${notification-rabbitmq-password}");
            });
    }

    @ParameterizedTest
    @CsvSource({
        "notification-rabbitmq-password, spring.rabbitmq.password, rabbitmq-password",
        "rabbitmq-password, notification.password, notification-rabbitmq-password"
    })
    void should_not_start_in_the_cluster_without_one_of_the_passwords(
        final String given, final String missingProperty, final String missingVariable
    ) {
        contextRunner
            .withPropertyValues("spring.profiles.active=home", given + "=secret")
            .run(context -> assertThat(context.getStartupFailure())
                .hasStackTraceContaining("The password of the broker is not set (" + missingProperty + ")")
                .hasStackTraceContaining("Could not resolve placeholder '" + missingVariable + "'"));
    }

    @Test
    void should_not_start_in_the_cluster_without_any_password() {
        contextRunner
            .withPropertyValues("spring.profiles.active=home")
            .run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @CsvSource({
        "rabbitmq-password, notification-rabbitmq-password, spring.rabbitmq.password",
        "notification-rabbitmq-password, rabbitmq-password, notification.password"
    })
    void should_not_start_with_an_empty_password(
        final String empty, final String given, final String emptyProperty
    ) {
        contextRunner
            .withPropertyValues("spring.profiles.active=home", empty + "=", given + "=secret")
            .run(context -> assertThat(context.getStartupFailure())
                .hasStackTraceContaining("The password of the broker is empty (" + emptyProperty + ")"));
    }

    @Test
    void should_start_in_the_cluster_with_the_passwords_of_the_deployment() {
        contextRunner
            .withPropertyValues(
                "spring.profiles.active=home",
                "rabbitmq-password=listenerSecret",
                "notification-rabbitmq-password=publisherSecret"
            )
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(RabbitProperties.class).getPassword()).isEqualTo("listenerSecret");
                assertThat(context.getBean(NotificationConnectionProperties.class).password())
                    .isEqualTo("publisherSecret");
            });
    }

    //a run on a workstation neither consumes nor publishes unless it is given the real passwords
    @Test
    void should_start_on_a_workstation_without_the_passwords() {
        contextRunner
            .withPropertyValues("spring.profiles.active=home,local")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(RabbitProperties.class).getPassword()).isEqualTo("dummyPassword");
                assertThat(context.getBean(NotificationConnectionProperties.class).password())
                    .isEqualTo("dummyPassword");
            });
    }

    @Test
    void should_take_the_passwords_given_to_a_run_on_a_workstation() {
        contextRunner
            .withPropertyValues(
                "spring.profiles.active=home,local",
                "rabbitmq-password=listenerSecret",
                "notification-rabbitmq-password=publisherSecret"
            )
            .run(context -> {
                assertThat(context.getBean(RabbitProperties.class).getPassword()).isEqualTo("listenerSecret");
                assertThat(context.getBean(NotificationConnectionProperties.class).password())
                    .isEqualTo("publisherSecret");
            });
    }

    @Test
    void should_start_under_the_test_profile() {
        contextRunner
            .withPropertyValues("spring.profiles.active=test")
            .run(context -> assertThat(context).hasNotFailed());
    }

    @Configuration
    @EnableConfigurationProperties({RabbitProperties.class, NotificationConnectionProperties.class})
    static class BrokerProperties {
    }
}
