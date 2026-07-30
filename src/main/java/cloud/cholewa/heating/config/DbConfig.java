package cloud.cholewa.heating.config;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@EnableR2dbcRepositories
@EnableConfigurationProperties(DatabaseProperties.class)
public class DbConfig {

    private static final int POOL_INITIAL_SIZE = 2;
    private static final int POOL_MAX_SIZE = 8;
    private static final Duration POOL_MAX_ACQUIRE_TIME = Duration.ofSeconds(10);
    private static final Duration POOL_MAX_IDLE_TIME = Duration.ofMinutes(5);

    private final DatabaseProperties databaseProperties;

    @Bean
    ConnectionFactory postgresConnectionFactory() {
        ConnectionFactory connectionFactory = ConnectionFactories.get(ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.DRIVER, "postgresql")
            .option(ConnectionFactoryOptions.HOST, databaseProperties.host())
            .option(ConnectionFactoryOptions.PORT, databaseProperties.port())
            .option(ConnectionFactoryOptions.DATABASE, databaseProperties.name())
            .option(ConnectionFactoryOptions.USER, databaseProperties.username())
            .option(ConnectionFactoryOptions.PASSWORD, databaseProperties.password())
            .option(Option.valueOf("sslMode"), "REQUIRE")
            .build()
        );

        //without the pool every query opens its own physical connection - a RabbitMQ backlog
        //exhausts the connection limit of the managed database within seconds
        return new ConnectionPool(ConnectionPoolConfiguration.builder(connectionFactory)
            .name("heating-service")
            .initialSize(POOL_INITIAL_SIZE)
            .maxSize(POOL_MAX_SIZE)
            .maxAcquireTime(POOL_MAX_ACQUIRE_TIME)
            .maxIdleTime(POOL_MAX_IDLE_TIME)
            .build()
        );
    }
}
