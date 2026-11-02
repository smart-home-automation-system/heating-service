package cloud.cholewa.heating.thermometer;

import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.service.TemperatureService;
import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.home.model.TemperatureMessage;
import cloud.cholewa.shelly.model.ShellyComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

//What a reading of a Shelly thermometer does to its room (HAS-360, HAS-364) - one way in for the Wi-Fi H&T, the
//status topics of a BLU H&T and the read of its gateway:
//- The temperature is a TemperatureMessage of the room, as an Eaton reading from amx-service: stored, the room
//  updated, the decision run (TemperatureService) - when Thermometers.accept takes it: never in a room with an Eaton
//  room sensor, never one not newer than the last one taken (the gateway is read again every five minutes).
//- The humidity is the humidity of the room, in memory (GET /rooms answers it), the newer one only.
//- A reading is dated when the device was heard - the time of the gateway for a BLU sensor, never later than now;
//  the time of the house when the device gave none
@Slf4j
@Component
@RequiredArgsConstructor
public class ThermometerReadings {

    //a reading a gateway still holds from longer ago is no reading of the room now: a BLU H&T sends about once a minute,
    //so one not heard for half an hour is silent (a dead battery, out of range) - the 24 h check of the sensors tells
    static final Duration STALE_AFTER = Duration.ofMinutes(30);

    private final Thermometers thermometers;
    private final BluThermometers bluThermometers;
    private final TemperatureService temperatureService;
    private final Home home;
    private final Clock clock;

    public Mono<Void> temperature(final RoomName room, final double celsius, final Instant heardAt) {
        return Mono.fromCallable(() -> measuredAt(heardAt))
            .filter(measuredAt -> !measuredAt.isBefore(LocalDateTime.now(clock).minus(STALE_AFTER)))
            .filter(measuredAt -> taken(room, celsius, measuredAt))
            .flatMap(measuredAt -> temperatureService.handleTemperature(new TemperatureMessage().room(room)
                .temperature(celsius).date(measuredAt)));
    }

    public Mono<Void> humidity(final RoomName room, final double percent, final Instant heardAt) {
        return Mono.justOrEmpty(room)
            .filter(thermometers::reads)
            .flatMap(known -> Mono.justOrEmpty(home.rooms().stream()
                .filter(candidate -> candidate.getName() == known && candidate.getHumidity() != null)
                .findFirst()))
            .flatMap(found -> Mono.fromCallable(() -> found.getHumidity().updateIfNewer(percent, measuredAt(heardAt))))
            .filter(Boolean::booleanValue)
            .doOnNext(updated -> log.info("Humidity: {} % for room: {}", percent, room.getValue()))
            .then();
    }

    public Mono<Void> reading(final BluThermometers.Reading reading) {
        return switch (reading.metric()) {
            case TEMPERATURE -> temperature(reading.room(), reading.value(), reading.heardAt());
            case HUMIDITY -> humidity(reading.room(), reading.value(), reading.heardAt());
        };
    }

    //the values the gateways hold, after their read: one room after the other, a failed one logged and the others go on
    public Mono<Void> fromGateways(final Map<String, List<ShellyComponent>> gateways) {
        return Flux.defer(() -> Flux.fromIterable(bluThermometers.apply(gateways)))
            .concatMap(reading -> reading(reading)
                .onErrorResume(failure -> Mono.fromRunnable(() -> log.warn(
                    "The BLU reading {} could not be taken: {}", reading, failure.toString()))))
            .then();
    }

    //the reading of a BLU H&T in a room on its Eaton sensor is logged, not taken - until HAS-365 removes that sensor
    private boolean taken(final RoomName room, final double celsius, final LocalDateTime measuredAt) {
        if (!thermometers.reads(room)) {
            log.info("Temperature {}°C for room: {} not taken: the room keeps its Eaton sensor", celsius, room.getValue());
            return false;
        }
        return thermometers.accept(room, measuredAt, held(room));
    }

    //when the reading the room holds was measured - the seeded one after a start - null when it has none
    private LocalDateTime held(final RoomName room) {
        return home.rooms().stream()
            .filter(candidate -> candidate.getName() == room && candidate.getTemperature() != null)
            .findFirst()
            .map(found -> found.getTemperature().snapshot().getUpdatedAt())
            .orElse(null);
    }

    private LocalDateTime measuredAt(final Instant heardAt) {
        final LocalDateTime now = LocalDateTime.now(clock);
        if (heardAt == null) {
            return now;
        }
        final LocalDateTime heard = LocalDateTime.ofInstant(heardAt, clock.getZone());
        return heard.isAfter(now) ? now : heard;
    }
}
