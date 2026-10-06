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

    @Bean
    MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    //the name the broker shows for the connection: the pod, which already says which service it
    //is and tells the old pod from the new one during a rollout. Kubernetes sets HOSTNAME;
    //outside the cluster there is none
    @Bean
    ConnectionNameStrategy connectionNameStrategy(
        @Value("${HOSTNAME:heating-service-local}") final String hostname
    ) {
        return connectionFactory -> hostname;
    }
}
