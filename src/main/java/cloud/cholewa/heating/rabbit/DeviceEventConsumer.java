package cloud.cholewa.heating.rabbit;

import cloud.cholewa.heating.window.WindowEventRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

//Every event of every device arrives here (HAS-232) - device.<env>.heating on /device, bound with
//"#", about 130 a minute - over the third connection of DeviceRabbitConfig. WindowEventRouter picks
//the windows by the routing key. Each one is logged at DEBUG only - at INFO the volume alone would
//drown the log
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceEventConsumer {

    private final WindowEventRouter windowEventRouter;

    @RabbitListener(queues = "${device.queue}", containerFactory = "deviceListenerContainerFactory")
    Mono<Void> consume(final Message message) {
        final String routingKey = message.getMessageProperties().getReceivedRoutingKey();

        return Mono.fromRunnable(() -> log.debug("Device event received with routing key: {}", routingKey))
            .then(Mono.defer(() -> windowEventRouter.route(routingKey, message.getBody())))
            //an event that cannot be handled is logged and acknowledged: a Mono that errors is
            //redelivered at once, which for a message that fails the same way is a tight loop
            .onErrorResume(throwable -> {
                log.error("Error while consuming the device event {}", routingKey, throwable);
                return Mono.empty();
            })
            //the listener observation lives in a ThreadLocal, but the container subscribes in a way that
            //does not trigger the automatic capture, so the reactor context stays empty and traceId is
            //lost on the first thread switch; this captures it at subscribe, where it is still present
            .contextCapture();
    }
}
