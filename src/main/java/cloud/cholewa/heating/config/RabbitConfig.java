package cloud.cholewa.heating.config;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//the name the broker shows for the connection - the pod - comes from cholewa-commons
//(RabbitConnectionNameAutoConfiguration); a ConnectionNameStrategy bean here would switch it off
@EnableRabbit
@Configuration
public class RabbitConfig {

    @Bean
    MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
