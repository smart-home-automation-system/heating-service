package cloud.cholewa.heating.config;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

//The connection to /device the events of the devices are consumed over (HAS-232). Built by hand and
//kept inside this object for the reason NotificationConnection of cholewa-commons is: a
//ConnectionFactory bean makes RabbitAutoConfiguration back off, and the temperature listener would
//lose the auto-configured connection with everything spring.rabbitmq.* sets on it. Declare this
//object as a bean, never the factory it holds.
//Only consumed over, so no confirms and no returns; the name is the one of the first connection
//of the pod with "/device" added
public final class DeviceConnection implements AutoCloseable {

    static final String CONNECTION_NAME_SUFFIX = "/device";

    private final CachingConnectionFactory connectionFactory;
    private final ApplicationContext applicationContext;

    private DeviceConnection(final CachingConnectionFactory connectionFactory, final ApplicationContext applicationContext) {
        this.connectionFactory = connectionFactory;
        this.applicationContext = applicationContext;
    }

    static DeviceConnection create(
        final RabbitProperties rabbitProperties,
        final DeviceConnectionProperties properties,
        final ConnectionNameStrategy connectionNameStrategy,
        final ApplicationContext applicationContext
    ) {
        final CachingConnectionFactory connectionFactory =
            new CachingConnectionFactory(rabbitProperties.determineHost(), rabbitProperties.determinePort());
        connectionFactory.setApplicationContext(applicationContext);
        connectionFactory.setVirtualHost(properties.virtualHost());
        connectionFactory.setUsername(properties.username());
        connectionFactory.setPassword(properties.password());
        connectionFactory.setConnectionNameStrategy(factory -> {
            final String name = connectionNameStrategy.obtainNewConnectionName(factory);
            return name == null ? null : name + CONNECTION_NAME_SUFFIX;
        });
        if (rabbitProperties.getConnectionTimeout() != null) {
            connectionFactory.setConnectionTimeout(Math.toIntExact(rabbitProperties.getConnectionTimeout().toMillis()));
        }
        if (rabbitProperties.getRequestedHeartbeat() != null) {
            connectionFactory.setRequestedHeartBeat(Math.toIntExact(rabbitProperties.getRequestedHeartbeat().toSeconds()));
        }
        return new DeviceConnection(connectionFactory, applicationContext);
    }

    //for the listener container factory of this package only - see the class comment
    ConnectionFactory connectionFactory() {
        return connectionFactory;
    }

    //destroy() alone only resets the connection; a factory built by hand stops for good only when it
    //is told that its context closed (the same as NotificationConnection.close())
    @Override
    public void close() {
        connectionFactory.onApplicationEvent(new ContextClosedEvent(applicationContext));
        connectionFactory.destroy();
    }
}
