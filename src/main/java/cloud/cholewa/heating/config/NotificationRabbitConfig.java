package cloud.cholewa.heating.config;

import cloud.cholewa.heating.rabbit.NotificationPublisher;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//the notification exchange lives on another virtual host, and a connection belongs to exactly one,
//so publishing there takes a second connection. Neither the connection factory nor the template is
//a bean, on purpose: RabbitAutoConfiguration backs off from any ConnectionFactory or
//RabbitOperations bean, and the temperature listener would lose the auto-configured connection
//together with everything spring.rabbitmq.* sets on it (manual ack, prefetch, observation)
@Slf4j
@Configuration
public class NotificationRabbitConfig {

    static final String NOTIFICATION_CONNECTION_SUFFIX = "/notification";

    private CachingConnectionFactory connectionFactory;

    @Bean
    NotificationPublisher notificationPublisher(
        final RabbitProperties rabbitProperties,
        final NotificationProperties notificationProperties,
        final ApplicationContext applicationContext,
        final ConnectionNameStrategy connectionNameStrategy
    ) {
        connectionFactory = new CachingConnectionFactory(
            rabbitProperties.determineHost(),
            rabbitProperties.determinePort()
        );
        connectionFactory.setVirtualHost(notificationProperties.virtualHost());
        connectionFactory.setUsername(notificationProperties.username());
        connectionFactory.setPassword(notificationProperties.password());
        //the second connection of the same pod: its name, from the strategy of the first, with
        //what this one is for
        connectionFactory.setConnectionNameStrategy(
            factory -> connectionNameStrategy.obtainNewConnectionName(factory) + NOTIFICATION_CONNECTION_SUFFIX);
        //a message matching no binding is dropped by the broker without a word; with returns it
        //comes back, and with correlated confirms the publisher learns the outcome of each send
        connectionFactory.setPublisherReturns(true);
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        if (rabbitProperties.getConnectionTimeout() != null) {
            connectionFactory.setConnectionTimeout((int) rabbitProperties.getConnectionTimeout().toMillis());
        }
        if (rabbitProperties.getRequestedHeartbeat() != null) {
            connectionFactory.setRequestedHeartBeat((int) rabbitProperties.getRequestedHeartbeat().toSeconds());
        }

        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReturnsCallback(returned -> log.error(
            "Notification was not routed to any queue: {} - {}", returned.getReplyCode(), returned.getReplyText()));
        //spring.rabbitmq.template.observation-enabled reaches only the auto-configured template;
        //this one finds the ObservationRegistry through the application context, so it needs both
        rabbitTemplate.setObservationEnabled(true);
        rabbitTemplate.setApplicationContext(applicationContext);
        rabbitTemplate.afterPropertiesSet();

        return new NotificationPublisher(rabbitTemplate, notificationProperties);
    }

    @PreDestroy
    void closeConnection() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }
}
