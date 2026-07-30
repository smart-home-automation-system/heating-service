package cloud.cholewa.heating.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.netty.LogbookClientHandler;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class AppConfig {

    @Bean
    ConnectionProvider connectionProvider() {
        return ConnectionProvider.create("shellyConnectionProvider");
    }

    @Bean
    HttpClient httpClient(final ConnectionProvider connectionProvider, final Logbook logbook) {
        return HttpClient.create(connectionProvider)
            .doOnConnected(connection -> connection.addHandlerLast(new LogbookClientHandler(logbook)));
    }

    @Bean
    WebClient shellyWebClient(final WebClient.Builder builder, final HttpClient httpClient) {
        return builder
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
