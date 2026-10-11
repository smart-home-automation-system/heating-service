package cloud.cholewa.heating;

import cloud.cholewa.commons.database.DatabaseProperties;
import cloud.cholewa.commons.notification.NotificationConnection;
import cloud.cholewa.commons.notification.NotificationPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.core.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class HeatingServiceApplicationTest {

    @Autowired
    private ApplicationContext context;
    @Autowired
    private DatabaseProperties databaseProperties;
    @Autowired
    private RabbitListenerEndpointRegistry listenerRegistry;
    @Autowired
    private NotificationConnection notificationConnection;

    @Test
    void contextLoads() {
    }

    //the pool is this service's share of the 22 connections of the managed database, counted twice
    //during a rollout; a lost or mistyped key falls back to the library default without a sound
    @Test
    void should_keep_the_connection_pool_at_its_share_of_the_database_budget() {
        assertThat(databaseProperties.pool().maxSize()).isEqualTo(2);
    }

    //the second connection, the one of the notifications, must stay out of the context: a
    //ConnectionFactory or a RabbitOperations bean makes RabbitAutoConfiguration back off, and the
    //listener is then left with a connection that knows nothing of spring.rabbitmq.*
    @Test
    void should_leave_the_listener_on_the_auto_configured_connection() {
        assertThat(context.getBeansOfType(ConnectionFactory.class)).hasSize(1);
        assertThat(context.getBeansOfType(RabbitOperations.class)).hasSize(1);

        final CachingConnectionFactory connectionFactory = context.getBean(CachingConnectionFactory.class);
        assertThat(connectionFactory.getVirtualHost()).isEqualTo("/temperature");
        assertThat(connectionFactory.getUsername()).isEqualTo("temperature");
        assertThat(context.getBean(RabbitOperations.class).getConnectionFactory()).isSameAs(connectionFactory);

        final Collection<MessageListenerContainer> containers = listenerRegistry.getListenerContainers();
        assertThat(containers).hasSize(1);

        final AbstractMessageListenerContainer container =
            (AbstractMessageListenerContainer) containers.iterator().next();
        assertThat(container.getConnectionFactory()).isSameAs(connectionFactory);
        //what spring.rabbitmq.listener.simple.* sets, and only on the auto-configured factory
        assertThat(container.getAcknowledgeMode()).isEqualTo(AcknowledgeMode.MANUAL);
        assertThat(ReflectionTestUtils.getField(container, "prefetchCount")).isEqualTo(3);
        assertThat(ReflectionTestUtils.getField(container, "observationEnabled")).isEqualTo(true);
        assertThat(ReflectionTestUtils.getField(context.getBean(RabbitOperations.class), "observationEnabled"))
            .isEqualTo(true);
    }

    @Test
    void should_take_the_connection_name_from_the_library() {
        assertThat(context.getBeanNamesForType(ConnectionNameStrategy.class))
            .containsExactly("cholewaPodConnectionNameStrategy");
    }

    //cholewa.notification.enabled stays off: the publisher that switch creates uses the connection
    //of the application, and here that one leads to /temperature, where no notification exchange is
    @Test
    void should_publish_notifications_over_the_second_connection_only() {
        assertThat(context.getEnvironment().getProperty("cholewa.notification.enabled", Boolean.class, false))
            .isFalse();
        assertThat(context.getBeanNamesForType(NotificationPublisher.class)).containsExactly("notificationPublisher");

        final ConnectionFactory notificationFactory = notificationConnection.rabbitOperations().getConnectionFactory();
        assertThat(notificationFactory).isNotSameAs(context.getBean(ConnectionFactory.class));
        assertThat(notificationFactory.getVirtualHost()).isEqualTo("/notification");
        assertThat(notificationFactory.getUsername()).isEqualTo("notification");
        assertThat(ReflectionTestUtils.getField(context.getBean(NotificationPublisher.class), "rabbitOperations"))
            .isSameAs(notificationConnection.rabbitOperations());
        assertThat(ReflectionTestUtils.getField(context.getBean(NotificationPublisher.class), "exchange"))
            .isEqualTo("notification");
        assertThat(ReflectionTestUtils.getField(context.getBean(NotificationPublisher.class), "env"))
            .isEqualTo("prod");
    }
}
