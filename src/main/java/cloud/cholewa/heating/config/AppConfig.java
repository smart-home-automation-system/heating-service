package cloud.cholewa.heating.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {

    @Bean
    WebClient webClient(
        final WebClient.Builder builder
    ) {
        return builder.build();
    }
}
