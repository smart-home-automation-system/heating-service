package cloud.cholewa.heating.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.netty.LogbookClientHandler;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class AppConfig {

    @Bean
    ConnectionProvider connectionProvider() {
        return ConnectionProvider.create("shellyConnectionProvider");
    }

    @Bean
    HttpClient httpClient(
        final ConnectionProvider connectionProvider,
        final Logbook logbook,
        @Value("${shelly.actor.connect-timeout}") final Duration connectTimeout,
        @Value("${shelly.actor.response-timeout}") final Duration responseTimeout
    ) {
        //without them a relay that accepts the connection and never answers holds the message that
        //asked for good, and with it one of the listener's prefetch slots. The response timeout is
        //netty's: the longest silence while the response is read, not a limit on the whole call
        return HttpClient.create(connectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(connectTimeout.toMillis()))
            .responseTimeout(responseTimeout)
            .doOnConnected(connection -> connection.addHandlerLast(new LogbookClientHandler(logbook)));
    }

    @Bean
    WebClient shellyWebClient(final WebClient.Builder builder, final HttpClient httpClient) {
        return builder
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
