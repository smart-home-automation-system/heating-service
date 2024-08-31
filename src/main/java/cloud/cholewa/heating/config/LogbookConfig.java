package cloud.cholewa.heating.config;

import cloud.cholewa.heating.config.logging.ConnectionProviderConfiguration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.netty.LogbookClientHandler;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Component
public class LogbookConfig {

    public ReactorClientHttpConnector reactorClientHttpConnector(
        final Logbook logbook,
        final Integer connectTimeout,
        final Long readTimeout,
        final ConnectionProviderConfiguration connectionProviderConfiguration
    ) {
        return new ReactorClientHttpConnector(
            getHttpClient(logbook, connectTimeout, readTimeout, connectionProviderConfiguration)
        );
    }

    private HttpClient getHttpClient(
        final Logbook logbook,
        final Integer connectionTimeout,
        final Long readTimeout,
        final ConnectionProviderConfiguration connectionProviderConfiguration
    ) {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("customConnectionProvider")
            .metrics(true)
            .build();

        return HttpClient.create(connectionProvider)
            .doOnConnected(connection -> {
                connection.addHandlerLast(
                    new LogbookClientHandler(logbook)
                );
            });
    }
}
