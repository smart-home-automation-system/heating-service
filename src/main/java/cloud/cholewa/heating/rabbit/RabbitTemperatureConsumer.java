package cloud.cholewa.heating.rabbit;

import cloud.cholewa.heating.service.TemperatureService;
import cloud.cholewa.home.model.TemperatureMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitTemperatureConsumer {

    private final TemperatureService temperatureService;

    @RabbitListener(queues = "${rabbitmq.queue.name}")
    Mono<Void> consumeTemperature(final TemperatureMessage message) {
        return temperatureService.handleTemperature(message)
            .doOnSubscribe(subscription ->
                log.info(
                    "Temperature message received for room: {} value: {}°C",
                    message.getRoom(),
                    message.getTemperature()
                ))
            .onErrorResume(throwable -> {
                log.error("Error while consuming temperature message: {}", throwable.getMessage());
                return Mono.empty();
            })
            //the listener observation lives in a ThreadLocal, but the container subscribes in a way that
            //does not trigger the automatic capture, so the reactor context stays empty and traceId is
            //lost on the first thread switch; this captures it at subscribe, where it is still present
            .contextCapture();
    }
}
