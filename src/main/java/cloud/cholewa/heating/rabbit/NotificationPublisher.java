package cloud.cholewa.heating.rabbit;

import cloud.cholewa.heating.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public class NotificationPublisher {

    static final String CATEGORY_HEADER = "category";
    static final String ENV_HEADER = "env";
    static final String CATEGORY_ALERT = "alert";
    static final String CATEGORY_INFO = "info";

    private final RabbitOperations rabbitOperations;
    private final NotificationProperties properties;

    public Mono<Void> publishAlert(final String text) {
        return publish(CATEGORY_ALERT, text);
    }

    public Mono<Void> publishInfo(final String text) {
        return publish(CATEGORY_INFO, text);
    }

    //a headers exchange: the routing key is ignored, the queue is chosen by category and env.
    //Plain text, because notification-service reads the message as a String - sent through a JSON
    //converter it would arrive quoted and as application/json
    private Mono<Void> publish(final String category, final String text) {
        return Mono.<Void>fromRunnable(() ->
                rabbitOperations.send(properties.exchange(), "", toMessage(category, text)))
            .doOnSuccess(unused -> log.info("Published {} notification: {}", category, text))
            //the send blocks on the broker connection
            .subscribeOn(Schedulers.boundedElastic());
    }

    private Message toMessage(final String category, final String text) {
        return MessageBuilder.withBody(text.getBytes(StandardCharsets.UTF_8))
            .setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN)
            .setContentEncoding(StandardCharsets.UTF_8.name())
            .setHeader(CATEGORY_HEADER, category)
            .setHeader(ENV_HEADER, properties.env())
            .build();
    }
}
