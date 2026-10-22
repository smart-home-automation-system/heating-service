package cloud.cholewa.heating.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class AppConfig {

    @Bean
    ConnectionProvider connectionProvider() {
        return ConnectionProvider.create("shellyConnectionProvider");
    }

    //No Logbook handler on the client any more (HAS-298): the request log comes from the
    //LogbookExchangeFilterFunction that cholewa-commons 1.10.0 puts on the injected builder, which
    //runs inside the reactive chain and so carries the traceId; the handler here logged every call a
    //second time, without it
    @Bean
    HttpClient httpClient(
        final ConnectionProvider connectionProvider,
        final ShellyTimeoutProperties timeouts
    ) {
        //without them a relay that accepts the connection and never answers holds the message that
        //asked for good, and with it one of the listener's prefetch slots. The response timeout is
        //netty's: the longest silence while the response is read, not a limit on the whole call
        return HttpClient.create(connectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(timeouts.connectTimeout().toMillis()))
            .responseTimeout(timeouts.responseTimeout());
    }

    @Bean
    WebClient shellyWebClient(final WebClient.Builder builder, final HttpClient httpClient) {
        return builder
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
