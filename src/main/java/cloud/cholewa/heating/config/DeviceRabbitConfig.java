package cloud.cholewa.heating.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//The events of the devices (HAS-232) are on /device, the readings of the temperature on
///temperature, and a connection belongs to one virtual host: the device listener takes a third
//connection, and a container factory of its own under another name than
//rabbitListenerContainerFactory - the auto-configured one, which the temperature listener keeps.
//What spring.rabbitmq.listener.simple.* sets reaches only that one, so it is set here again
@Configuration
public class DeviceRabbitConfig {

    //about 130 events a minute, each decided by the routing key in microseconds; the body of one in
    //a hundred is read
    static final int PREFETCH = 10;

    //the same broker as spring.rabbitmq.*, with its connection timeout and heartbeat. Closed with
    //the context: Spring finds close() of an AutoCloseable bean by itself
    @Bean
    DeviceConnection deviceConnection(
        final RabbitProperties rabbitProperties,
        final DeviceConnectionProperties properties,
        final ConnectionNameStrategy connectionNameStrategy,
        final ApplicationContext applicationContext
    ) {
        return DeviceConnection.create(rabbitProperties, properties, connectionNameStrategy, applicationContext);
    }

    @Bean
    SimpleRabbitListenerContainerFactory deviceListenerContainerFactory(final DeviceConnection deviceConnection) {
        final SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(deviceConnection.connectionFactory());
        //the listener returns a Mono: with AUTO the container would ack before the event is handled
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(PREFETCH);
        //without it the trace of an event would start in the listener
        factory.setObservationEnabled(true);
        return factory;
    }
}
