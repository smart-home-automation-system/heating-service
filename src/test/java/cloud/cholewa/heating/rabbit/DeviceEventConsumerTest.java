package cloud.cholewa.heating.rabbit;

import cloud.cholewa.heating.window.WindowEventRouter;
import cloud.cholewa.shelly.codec.ShellyDecodeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceEventConsumerTest {

    private final WindowEventRouter router = mock(WindowEventRouter.class);
    private final DeviceEventConsumer consumer = new DeviceEventConsumer(router);

    //an event that completes is acknowledged; one that errored would be redelivered at once
    @ParameterizedTest
    @ValueSource(strings = {"shellyplus1pm-a1b2c3.status.switch:0", "eaton.blinds.45", ""})
    void should_take_every_event_off_the_queue(final String routingKey) {
        when(router.route(any(), any())).thenReturn(Mono.empty());

        consumer.consume(message(routingKey)).as(StepVerifier::create).verifyComplete();

        verify(router).route(eq(routingKey), any());
    }

    @Test
    void should_complete_an_event_that_could_not_be_handled() {
        when(router.route(any(), any())).thenReturn(Mono.error(new ShellyDecodeException("Not the status of bthomesensor:215")));

        consumer.consume(message("shellyplugsg3-28372f2f0460.status.bthomesensor:215")).as(StepVerifier::create).verifyComplete();
    }

    //a router that throws before it returns a Mono is an error of the chain all the same
    @Test
    void should_complete_an_event_whose_router_threw() {
        when(router.route(any(), any())).thenThrow(new IllegalStateException("thrown"));

        consumer.consume(message("eaton.blinds.9")).as(StepVerifier::create).verifyComplete();
    }

    private static Message message(final String routingKey) {
        final MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey(routingKey);
        return new Message("{}".getBytes(StandardCharsets.UTF_8), properties);
    }
}
