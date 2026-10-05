package cloud.cholewa.heating.rabbit;

import cloud.cholewa.heating.config.NotificationProperties;
import cloud.cholewa.heating.infrastructure.error.HeatingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationPublisherTest {

    @Mock
    private RabbitOperations rabbitOperations;
    @Captor
    private ArgumentCaptor<Message> messageCaptor;

    private NotificationPublisher sut;

    @BeforeEach
    void setUp() {
        sut = new NotificationPublisher(
            rabbitOperations,
            new NotificationProperties("/notification", "notification", "secret", "notification", "dev")
        );
    }

    @Test
    void should_publish_alert_as_plain_text_routed_by_headers() {
        brokerAnswers(correlationData -> correlationData.getFuture().complete(new CorrelationData.Confirm(true, null)));

        sut.publishAlert("Sensor in room óffice is not reporting").as(StepVerifier::create).verifyComplete();

        verify(rabbitOperations).send(eq("notification"), eq(""), messageCaptor.capture(), any(CorrelationData.class));

        final Message message = messageCaptor.getValue();
        assertThat(new String(message.getBody(), StandardCharsets.UTF_8))
            .isEqualTo("Sensor in room óffice is not reporting");
        assertThat(message.getMessageProperties().getContentType()).isEqualTo("text/plain");
        assertThat(message.getMessageProperties().getContentEncoding()).isEqualTo("UTF-8");
        assertThat(message.getMessageProperties().getHeaders())
            .containsEntry("category", "alert")
            .containsEntry("env", "dev");
    }

    @Test
    void should_publish_info_with_info_category() {
        brokerAnswers(correlationData -> correlationData.getFuture().complete(new CorrelationData.Confirm(true, null)));

        sut.publishInfo("Sensor is back").as(StepVerifier::create).verifyComplete();

        verify(rabbitOperations).send(eq("notification"), eq(""), messageCaptor.capture(), any(CorrelationData.class));

        assertThat(messageCaptor.getValue().getMessageProperties().getHeaders())
            .containsEntry("category", "info")
            .containsEntry("env", "dev");
    }

    @Test
    void should_signal_error_when_broker_is_unreachable() {
        doThrow(new AmqpConnectException(new RuntimeException("connection refused")))
            .when(rabbitOperations).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        sut.publishAlert("Sensor is not reporting")
            .as(StepVerifier::create)
            .verifyError(AmqpConnectException.class);
    }

    @Test
    void should_signal_error_when_broker_refuses_the_message() {
        brokerAnswers(correlationData ->
            correlationData.getFuture().complete(new CorrelationData.Confirm(false, "exchange not found")));

        sut.publishAlert("Sensor is not reporting")
            .as(StepVerifier::create)
            .verifyErrorMatches(throwable -> throwable instanceof HeatingException
                && throwable.getMessage().equals("Notification refused by the broker: exchange not found"));
    }

    //the broker confirms an unroutable message as well - only the return says it reached no queue
    @Test
    void should_signal_error_when_message_matches_no_queue() {
        brokerAnswers(correlationData -> {
            correlationData.setReturned(new ReturnedMessage(
                new Message(new byte[0]), 312, "NO_ROUTE", "notification", ""));
            correlationData.getFuture().complete(new CorrelationData.Confirm(true, null));
        });

        sut.publishAlert("Sensor is not reporting")
            .as(StepVerifier::create)
            .verifyErrorMatches(throwable -> throwable instanceof HeatingException
                && throwable.getMessage().equals("Notification was not routed to any queue: NO_ROUTE"));
    }

    @Test
    void should_signal_error_when_broker_never_confirms() {
        StepVerifier.withVirtualTime(() -> sut.publishAlert("Sensor is not reporting"))
            .thenAwait(NotificationPublisher.CONFIRM_TIMEOUT.plus(Duration.ofSeconds(1)))
            .verifyError(TimeoutException.class);
    }

    private void brokerAnswers(final Consumer<CorrelationData> outcome) {
        doAnswer(invocation -> {
            outcome.accept(invocation.getArgument(3));
            return null;
        }).when(rabbitOperations).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
    }
}
