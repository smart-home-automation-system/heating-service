package cloud.cholewa.heating.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableRabbit
@Configuration
public class RabbitConfig {

    @Value("${HOSTNAME:local}")
    private String hostname;

    @Bean
    MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    ConnectionNameStrategy connectionNameStrategy() {
        return connectionFactory -> "heating-service-" + hostname;
    }
}
