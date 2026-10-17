package cloud.cholewa.heating.config;

import cloud.cholewa.shelly.http.ShellyHttpClientCustomizer;
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

//The client of database-service (HAS-232), with its own timeouts and its own pool - not the one of
//the relays in AppConfig. Built from the injected builder, or the calls would be untraced
@Configuration
public class RegistryWebClientConfig {

    //Without a limit an idle connection stays in the pool for ever: after a restart of the target,
    //or once conntrack has forgotten it, the next call is written onto a dead socket (the lesson of
    //the gateway, HAS-212). The same limits as there
    static final Duration MAX_IDLE_TIME = Duration.ofMinutes(2);
    static final Duration MAX_LIFE_TIME = Duration.ofMinutes(30);

    //Spring infers only close() and shutdown() as destroy methods, so the pool is named explicitly
    @Bean(destroyMethod = "dispose")
    ConnectionProvider databaseConnectionProvider() {
        return ConnectionProvider.builder("database-service")
            .maxIdleTime(MAX_IDLE_TIME)
            .maxLifeTime(MAX_LIFE_TIME)
            .build();
    }

    @Bean
    WebClient databaseWebClient(
        final WebClient.Builder webClientBuilder,
        final ConnectionProvider databaseConnectionProvider,
        final Logbook logbook,
        final InternalServiceProperties properties
    ) {
        final InternalServiceProperties.Endpoint endpoint = properties.database();
        final HttpClient httpClient = HttpClient.create(databaseConnectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(endpoint.connectTimeout().toMillis()))
            .responseTimeout(endpoint.responseTimeout())
            .doOnConnected(connection -> connection.addHandlerLast(new LogbookClientHandler(logbook)));

        return webClientBuilder
            .baseUrl(endpoint.url())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }

    //the clients of shelly-client - the gateways are read through its ShellyRpcClient - put their
    //own connector on the builder, so the Logbook handlers above do not reach them; this does
    @Bean
    ShellyHttpClientCustomizer shellyCallsInTheLog(final Logbook logbook) {
        return http -> http.doOnConnected(connection -> connection.addHandlerLast(new LogbookClientHandler(logbook)));
    }
}
