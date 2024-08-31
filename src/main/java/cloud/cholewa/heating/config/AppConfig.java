package cloud.cholewa.heating.config;

import cloud.cholewa.heating.config.logging.ConnectionProviderConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.zalando.logbook.Logbook;

@Configuration
@RequiredArgsConstructor
public class AppConfig {

    private final LogbookConfig logbookConfig;

    @Bean
    WebClient hotWaterSensorWebClient(
        final WebClient.Builder builder,
        final Logbook logbook
        ) {
        return builder.clientConnector(logbookConfig.reactorClientHttpConnector(
                logbook, 10000, 30000L,
                new ConnectionProviderConfiguration(180000L, 10, 50000L, 300000L)
            ))
            .build();
    }
}
