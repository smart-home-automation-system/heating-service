package cloud.cholewa.heating.window;

import cloud.cholewa.shelly.codec.ShellyEvent;
import cloud.cholewa.shelly.codec.ShellyMqttDecoder;
import cloud.cholewa.shelly.codec.ShellyTopic;
import cloud.cholewa.shelly.model.BthomeSensorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

//What of the device queue is about a window (HAS-232). The queue carries every event of every device,
//about 130 a minute: the routing key alone decides first, so the body of an event that is not a
//window is never read - only <gatewayId>.status.bthomesensor:<id> of a component the window sensors
//know, the state of a window, {id, value, last_updated_ts}, published by the gateway when it
//changes. A body that is not what its topic carries is an error for the consumer to log
@Component
@RequiredArgsConstructor
public class WindowEventRouter {

    private static final String BTHOME_SENSOR = "bthomesensor";

    private final ShellyMqttDecoder shellyMqttDecoder;
    private final WindowSensors windowSensors;

    public Mono<Void> route(final String routingKey, final byte[] body) {
        if (routingKey == null) {
            return Mono.empty();
        }
        return ShellyTopic.parse(routingKey)
            .filter(topic -> topic.kind() == ShellyTopic.Kind.STATUS && BTHOME_SENSOR.equals(topic.component())
                && windowSensors.knows(topic.deviceId(), topic.index()))
            .map(topic -> window(topic, body))
            .orElseGet(Mono::empty);
    }

    private Mono<Void> window(final ShellyTopic topic, final byte[] body) {
        return Mono.fromCallable(() -> shellyMqttDecoder.decode(topic, body))
            .flatMap(Mono::justOrEmpty)
            .cast(ShellyEvent.Status.class)
            .flatMap(status -> Mono.justOrEmpty(status.as(BthomeSensorStatus.class)))
            .doOnNext(status -> windowSensors.observe(topic.deviceId(), topic.index(), status))
            .then();
    }
}
