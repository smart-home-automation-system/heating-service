package cloud.cholewa.heating.thermometer;

import cloud.cholewa.commons.scheduling.ScheduledPass;
import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.shelly.http.ShellyGen1Client;
import cloud.cholewa.shelly.model.ExternalHumidityValue;
import cloud.cholewa.shelly.model.ExternalTemperatureValue;
import cloud.cholewa.shelly.model.Gen1DeviceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

//The Gen1 thermometers of the rooms (HAS-368) - the Shelly 1 with the temperature add-on of the sauna, which can only
//be polled: every heating.thermometer.gen1-poll (PT1M) its /status, sensor 0 of ext_temperature handed to
//ThermometerReadings as a reading of now - the way of the Wi-Fi H&T, so the Eaton rule and the newer-wins rule of
//Thermometers.accept hold; the same value only every REPEAT_AFTER (10 min), a change at once; ext_humidity the same way
//(every poll - it lives in memory) when the add-on has a DHT22. One device after the other, each with
//its own error handling: a device that does not answer, or answers without a temperature, is a failed read - a WARN at
//most once an hour per device and an INFO on its return - and the last reading of the room holds (the 24 h silence
//check of the sensors covers a dead device). Bounded at LIMIT, below the shortest poll
@Slf4j
@Component
public class Gen1Thermometers {

    static final Duration LIMIT = Duration.ofSeconds(25);
    static final Duration WARN_AGAIN_AFTER = Duration.ofHours(1);
    static final Duration REPEAT_AFTER = Duration.ofMinutes(10);
    private static final String FIRST_SENSOR = "0";

    private final Thermometers thermometers;
    private final ThermometerReadings thermometerReadings;
    private final ShellyGen1Client shellyGen1Client;
    private final Clock clock;
    //when the failure of a device was last told, by its address
    private final Map<String, Instant> told = new ConcurrentHashMap<>();
    //the temperature last handed on from each device and when
    private final Map<String, Handed> handed = new ConcurrentHashMap<>();

    private record Handed(double celsius, Instant at) {
    }

    public Gen1Thermometers(
        final Thermometers thermometers,
        final ThermometerReadings thermometerReadings,
        final ShellyGen1Client standaloneHeaterClient,
        final Clock clock
    ) {
        this.thermometers = thermometers;
        this.thermometerReadings = thermometerReadings;
        this.shellyGen1Client = standaloneHeaterClient;
        this.clock = clock;
    }

    //timeUnit: a bare number of the poll is seconds here too, as ThermometerProperties binds it
    @Scheduled(initialDelayString = "PT30S", fixedDelayString = "${heating.thermometer.gen1-poll}",
        timeUnit = TimeUnit.SECONDS)
    Mono<Void> poll() {
        return ScheduledPass.bounded("Gen1 thermometers", LIMIT, () -> Flux.defer(() ->
                Flux.fromIterable(thermometers.gen1().entrySet()))
            .concatMap(device -> read(device.getKey(), device.getValue()))
            .then());
    }

    private Mono<Void> read(final String address, final RoomName room) {
        return shellyGen1Client.status(address)
            .flatMap(status -> Mono.justOrEmpty(temperature(status))
                .switchIfEmpty(Mono.error(() -> new IllegalStateException("no temperature of its add-on")))
                .filter(celsius -> changedOrDue(address, celsius))
                .flatMap(celsius -> thermometerReadings.temperature(room, celsius, null)
                    .then(Mono.fromRunnable(() -> handed.put(address, new Handed(celsius, clock.instant())))))
                .then(Mono.justOrEmpty(humidity(status))
                    .flatMap(percent -> thermometerReadings.humidity(room, percent, null))))
            .doOnSuccess(done -> returned(address, room))
            .onErrorResume(failure -> Mono.fromRunnable(() -> failed(address, room, failure)));
    }

    //a value is handed on when it changed, or the same one again once REPEAT_AFTER passed - enough for the 24 h silence
    //check and the history (buckets of 20 min at least); a poll a minute would otherwise store 1440 rows a day
    private boolean changedOrDue(final String address, final double celsius) {
        final Handed last = handed.get(address);
        return last == null || last.celsius() != celsius
            || !clock.instant().isBefore(last.at().plus(REPEAT_AFTER));
    }

    private static Optional<Double> temperature(final Gen1DeviceStatus status) {
        return Optional.ofNullable(status.getExtTemperature())
            .map(sensors -> sensors.get(FIRST_SENSOR))
            .map(ExternalTemperatureValue::gettC);
    }

    private static Optional<Double> humidity(final Gen1DeviceStatus status) {
        return Optional.ofNullable(status.getExtHumidity())
            .map(sensors -> sensors.get(FIRST_SENSOR))
            .map(ExternalHumidityValue::getHum);
    }

    private void failed(final String address, final RoomName room, final Throwable failure) {
        final Instant now = clock.instant();
        final Instant before = told.get(address);
        if (before == null || !now.isBefore(before.plus(WARN_AGAIN_AFTER))) {
            told.put(address, now);
            log.warn("The Gen1 thermometer {} of room {} could not be read, the last reading holds: {}", address,
                room.getValue(), failure.toString());
        }
    }

    private void returned(final String address, final RoomName room) {
        if (told.remove(address) != null) {
            log.info("The Gen1 thermometer {} of room {} is read again", address, room.getValue());
        }
    }
}
