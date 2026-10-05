package cloud.cholewa.heating.rabbit;

import cloud.cholewa.heating.config.NotificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
        sut.publishAlert("Sensor in room óffice is not reporting").as(StepVerifier::create).verifyComplete();

        verify(rabbitOperations).send(eq("notification"), eq(""), messageCaptor.capture());

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
        sut.publishInfo("Sensor is back").as(StepVerifier::create).verifyComplete();

        verify(rabbitOperations).send(eq("notification"), eq(""), messageCaptor.capture());

        assertThat(messageCaptor.getValue().getMessageProperties().getHeaders())
            .containsEntry("category", "info")
            .containsEntry("env", "dev");
    }

    @Test
    void should_signal_error_when_broker_is_unreachable() {
        doThrow(new AmqpConnectException(new RuntimeException("connection refused")))
            .when(rabbitOperations).send(anyString(), anyString(), any(Message.class));

        sut.publishAlert("Sensor is not reporting")
            .as(StepVerifier::create)
            .verifyError(AmqpConnectException.class);
    }
}
