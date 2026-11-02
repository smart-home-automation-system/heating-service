package cloud.cholewa.heating.thermometer;

import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.shelly.codec.ShellyEvent;
import cloud.cholewa.shelly.codec.ShellyMqttDecoder;
import cloud.cholewa.shelly.codec.ShellyTopic;
import cloud.cholewa.shelly.model.BthomeSensorStatus;
import cloud.cholewa.shelly.model.HumidityStatus;
import cloud.cholewa.shelly.model.TemperatureStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

//What of the device queue is a reading of a Shelly thermometer. The queue carries every event of every device: the
//routing key decides first, so the body of an event that is not a thermometer is never read.
//- A Wi-Fi thermometer (HAS-360): <deviceId>.status.temperature:0 and humidity:0 of one Thermometers knows - a Plus
//  H&T publishes them at every wake-up, every two hours or on a change; dated now.
//- A BLU H&T (HAS-364): <gatewayId>.status.bthomesensor:<id> of a component BluThermometers found to be its
//  temperature or humidity - {id, value, last_updated_ts}, published by the gateway on a change; dated by the gateway.
//What a reading does to its room is ThermometerReadings'. A body that is not what its topic carries is an error for
//the consumer to log; a status without a value is skipped
@Component
@RequiredArgsConstructor
public class ThermometerEventRouter {

    private static final String TEMPERATURE = "temperature";
    private static final String HUMIDITY = "humidity";
    private static final String BTHOME_SENSOR = "bthomesensor";
    private static final int CHANNEL = 0;

    private final ShellyMqttDecoder shellyMqttDecoder;
    private final Thermometers thermometers;
    private final BluThermometers bluThermometers;
    private final ThermometerReadings thermometerReadings;

    public Mono<Void> route(final String routingKey, final byte[] body) {
        return Mono.justOrEmpty(routingKey)
            .flatMap(key -> Mono.justOrEmpty(ShellyTopic.parse(key)))
            .filter(topic -> topic.kind() == ShellyTopic.Kind.STATUS)
            .flatMap(topic -> blu(topic, body)
                .then(Mono.defer(() -> temperature(topic, body)))
                .then(Mono.defer(() -> humidity(topic, body))));
    }

    private Mono<Void> blu(final ShellyTopic topic, final byte[] body) {
        return Mono.just(topic)
            .filter(candidate -> BTHOME_SENSOR.equals(candidate.component()))
            .flatMap(candidate -> Mono.justOrEmpty(bluThermometers.target(candidate.deviceId(), candidate.index())))
            .flatMap(target -> status(topic, body, BthomeSensorStatus.class)
                .flatMap(status -> Mono.justOrEmpty(BluThermometers.reading(target, status))))
            .flatMap(thermometerReadings::reading);
    }

    private Mono<Void> temperature(final ShellyTopic topic, final byte[] body) {
        return wifiRoom(topic, TEMPERATURE)
            .flatMap(room -> status(topic, body, TemperatureStatus.class)
                .flatMap(status -> Mono.justOrEmpty(status.gettC()))
                .flatMap(celsius -> thermometerReadings.temperature(room, celsius, null)));
    }

    private Mono<Void> humidity(final ShellyTopic topic, final byte[] body) {
        return wifiRoom(topic, HUMIDITY)
            .flatMap(room -> status(topic, body, HumidityStatus.class)
                .flatMap(status -> Mono.justOrEmpty(status.getRh()))
                .flatMap(percent -> thermometerReadings.humidity(room, percent, null)));
    }

    //the room of a Wi-Fi thermometer whose channel 0 of this component the topic is
    private Mono<RoomName> wifiRoom(final ShellyTopic topic, final String component) {
        return Mono.just(topic)
            .filter(candidate -> component.equals(candidate.component()) && Integer.valueOf(CHANNEL).equals(candidate.index()))
            .flatMap(candidate -> Mono.justOrEmpty(thermometers.roomOf(candidate.deviceId())));
    }

    private <T> Mono<T> status(final ShellyTopic topic, final byte[] body, final Class<T> type) {
        return Mono.fromCallable(() -> shellyMqttDecoder.decode(topic, body))
            .flatMap(Mono::justOrEmpty)
            .cast(ShellyEvent.Status.class)
            .flatMap(status -> Mono.justOrEmpty(status.as(type)));
    }
}
