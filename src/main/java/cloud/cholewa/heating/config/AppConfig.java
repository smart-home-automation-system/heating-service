package cloud.cholewa.heating.config;

import io.netty.channel.ChannelOption;
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

    static final int SHELLY_CONNECT_TIMEOUT_MILLIS = 10_000;
    static final Duration SHELLY_RESPONSE_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    ConnectionProvider connectionProvider() {
        return ConnectionProvider.create("shellyConnectionProvider");
    }

    @Bean
    HttpClient httpClient(final ConnectionProvider connectionProvider, final Logbook logbook) {
        //without them a relay that accepts the connection and never answers holds the message that
        //asked for good, and with it one of the listener's prefetch slots. A timeout ends the call
        //as any other device error does: the room's pass stops there, nothing is written to the
        //state, and the next reading of the room asks the device again
        return HttpClient.create(connectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, SHELLY_CONNECT_TIMEOUT_MILLIS)
            .responseTimeout(SHELLY_RESPONSE_TIMEOUT)
            .doOnConnected(connection -> connection.addHandlerLast(new LogbookClientHandler(logbook)));
    }

    @Bean
    WebClient shellyWebClient(final WebClient.Builder builder, final HttpClient httpClient) {
        return builder
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
