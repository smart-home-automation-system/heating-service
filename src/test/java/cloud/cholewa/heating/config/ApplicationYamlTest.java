package cloud.cholewa.heating.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

//application.yaml as the profiles read it, and nothing else: no auto-configuration and no bean but
//the properties of the notifications, so nothing here can open a connection. The test document
//points the context tests at dead addresses and at names of its own - this is what keeps a broken
//value of production from hiding behind it
class ApplicationYamlTest {

    private static final String[] SHELLY_HOSTS = {
        "shelly.actor.pro4.down_left.host",
        "shelly.actor.pro4.down_right.host",
        "shelly.actor.pro4.up_left.host",
        "shelly.actor.pro4.up_right.host"
    };

    //the three passwords are what the deployment sets, under these names. The active profiles are
    //given here, above the system property surefire sets for every test class
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withUserConfiguration(Properties.class)
        .withPropertyValues(
            "rabbitmq-password=listenerSecret",
            "notification-rabbitmq-password=publisherSecret",
            "device-rabbitmq-password=deviceSecret"
        );

    @Test
    void should_read_the_listener_and_the_notifications_of_production_in_the_cluster() {
        contextRunner
            .withPropertyValues("spring.profiles.active=home")
            .run(context -> {
                final Environment environment = context.getEnvironment();

                assertThat(environment.getActiveProfiles()).containsExactly("home");
                assertThat(environment.getProperty("spring.rabbitmq.host")).isEqualTo("rabbitmq-headless");
                assertThat(environment.getProperty("spring.rabbitmq.port")).isEqualTo("5672");
                assertThat(environment.getProperty("spring.rabbitmq.virtual-host")).isEqualTo("/temperature");
                assertThat(environment.getProperty("spring.rabbitmq.username")).isEqualTo("temperature");
                assertThat(environment.getProperty("spring.rabbitmq.password")).isEqualTo("listenerSecret");
                assertThat(environment.getProperty("rabbitmq.queue.name")).isEqualTo("temperature.prod.heating");
                assertThat(environment.getProperty("cholewa.notification.enabled")).isNull();

                assertThat(context.getBean(NotificationConnectionProperties.class)).isEqualTo(
                    new NotificationConnectionProperties(
                        "/notification", "notification", "publisherSecret", "notification", "prod"));

                //the events of the devices and the registry of the window sensors (HAS-232)
                assertThat(context.getBean(DeviceConnectionProperties.class)).isEqualTo(
                    new DeviceConnectionProperties("/device", "device", "deviceSecret", "device.prod.heating"));
                assertThat(context.getBean(InternalServiceProperties.class).database()).isEqualTo(
                    new InternalServiceProperties.Endpoint("http://database-service:6200", Duration.ofSeconds(3), Duration.ofSeconds(5)));
                assertThat(context.getBean(WindowProperties.class))
                    .isEqualTo(new WindowProperties(Duration.ofMinutes(3), Duration.ofMinutes(30)));

                //the relays of the house, not the dead address of the test document
                assertThat(environment.getProperty("shelly.actor.port")).isEqualTo("80");
                assertThat(shellyHosts(environment)).hasSize(4).doesNotContain("localhost", "", null);
                assertThat(environment.getProperty("database.port")).isEqualTo("5432");
            });
    }

    @Test
    void should_read_the_queue_and_the_env_of_development_on_a_workstation() {
        contextRunner
            .withPropertyValues("spring.profiles.active=home,local")
            .run(context -> {
                final Environment environment = context.getEnvironment();

                assertThat(environment.getProperty("spring.rabbitmq.host")).isEqualTo("localhost");
                assertThat(environment.getProperty("rabbitmq.queue.name")).isEqualTo("temperature.dev.heating");
                assertThat(context.getBean(NotificationConnectionProperties.class).env()).isEqualTo("dev");
                assertThat(context.getBean(DeviceConnectionProperties.class).queue()).isEqualTo("device.dev.heating");
                assertThat(context.getBean(InternalServiceProperties.class).database().url())
                    .isEqualTo("http://localhost:6005");
            });
    }

    @Test
    void should_keep_a_test_context_away_from_the_broker_the_database_and_the_relays() {
        contextRunner
            .withPropertyValues("spring.profiles.active=test", "database-host=database.example")
            .run(context -> {
                final Environment environment = context.getEnvironment();

                assertThat(environment.getProperty("spring.rabbitmq.host")).isEqualTo("localhost");
                assertThat(environment.getProperty("spring.rabbitmq.port")).isEqualTo("1");
                assertThat(environment.getProperty("rabbitmq.queue.name")).isEqualTo("temperature.test.heating");
                assertThat(context.getBean(NotificationConnectionProperties.class).env()).isEqualTo("test");
                assertThat(context.getBean(DeviceConnectionProperties.class).queue()).isEqualTo("device.test.heating");
                assertThat(context.getBean(InternalServiceProperties.class).database().url())
                    .isEqualTo("http://localhost:1");
                //whatever the environment of the machine says
                assertThat(environment.getProperty("database.host")).isEqualTo("localhost");
                assertThat(environment.getProperty("database.port")).isEqualTo("1");
                assertThat(environment.getProperty("shelly.actor.port")).isEqualTo("1");
                assertThat(shellyHosts(environment)).hasSize(4).containsOnly("localhost");
                assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("false");
            });
    }

    private static List<String> shellyHosts(final Environment environment) {
        return Stream.of(SHELLY_HOSTS).map(environment::getProperty).toList();
    }

    @Configuration
    @EnableConfigurationProperties({
        NotificationConnectionProperties.class,
        DeviceConnectionProperties.class,
        InternalServiceProperties.class,
        WindowProperties.class
    })
    static class Properties {
    }
}
