package cloud.cholewa.heating.thermometer;

import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.service.TemperatureService;
import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.home.model.TemperatureMessage;
import cloud.cholewa.shelly.codec.ShellyEvent;
import cloud.cholewa.shelly.codec.ShellyMqttDecoder;
import cloud.cholewa.shelly.codec.ShellyTopic;
import cloud.cholewa.shelly.model.HumidityStatus;
import cloud.cholewa.shelly.model.TemperatureStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDateTime;

//What of the device queue is a reading of a Shelly thermometer (HAS-360). The queue carries every event of every
//device: the routing key decides first, so only <deviceId>.status.temperature:0 and humidity:0 of a thermometer
//Thermometers knows are read at all. A Plus H&T publishes them at every wake-up - every two hours or on a change.
//- The temperature (tC) is a TemperatureMessage of the room, as an Eaton reading from amx-service: stored, the room
//  updated, the decision run (TemperatureService).
//- The humidity (rh) is the humidity of the room, in memory - the first one the service has (GET /rooms answers it).
//A body that is not what its topic carries is an error for the consumer to log; a status without a value is skipped
@Slf4j
@Component
@RequiredArgsConstructor
public class ThermometerEventRouter {

    private static final String TEMPERATURE = "temperature";
    private static final String HUMIDITY = "humidity";
    private static final int CHANNEL = 0;

    private final ShellyMqttDecoder shellyMqttDecoder;
    private final Thermometers thermometers;
    private final TemperatureService temperatureService;
    private final Home home;
    private final Clock clock;

    public Mono<Void> route(final String routingKey, final byte[] body) {
        return Mono.justOrEmpty(routingKey)
            .flatMap(key -> Mono.justOrEmpty(ShellyTopic.parse(key)))
            .filter(topic -> topic.kind() == ShellyTopic.Kind.STATUS
                && (TEMPERATURE.equals(topic.component()) || HUMIDITY.equals(topic.component()))
                && Integer.valueOf(CHANNEL).equals(topic.index()))
            .flatMap(topic -> Mono.justOrEmpty(thermometers.roomOf(topic.deviceId()))
                .flatMap(room -> TEMPERATURE.equals(topic.component())
                    ? temperature(topic, body, room)
                    : humidity(topic, body, room)));
    }

    private Mono<Void> temperature(final ShellyTopic topic, final byte[] body, final RoomName room) {
        return status(topic, body, TemperatureStatus.class)
            .flatMap(status -> Mono.justOrEmpty(status.gettC()))
            .flatMap(celsius -> temperatureService.handleTemperature(new TemperatureMessage()
                .room(room)
                .temperature(celsius)
                .date(LocalDateTime.now(clock))));
    }

    private Mono<Void> humidity(final ShellyTopic topic, final byte[] body, final RoomName room) {
        return status(topic, body, HumidityStatus.class)
            .flatMap(status -> Mono.justOrEmpty(status.getRh()))
            .doOnNext(percent -> home.rooms().stream()
                .filter(candidate -> candidate.getName() == room && candidate.getHumidity() != null)
                .findFirst()
                .ifPresent(found -> {
                    found.getHumidity().update(percent, LocalDateTime.now(clock));
                    log.info("Humidity: {} % for room: {}", percent, room.getValue());
                }))
            .then();
    }

    private <T> Mono<T> status(final ShellyTopic topic, final byte[] body, final Class<T> type) {
        return Mono.fromCallable(() -> shellyMqttDecoder.decode(topic, body))
            .flatMap(Mono::justOrEmpty)
            .cast(ShellyEvent.Status.class)
            .flatMap(status -> Mono.justOrEmpty(status.as(type)));
    }
}
