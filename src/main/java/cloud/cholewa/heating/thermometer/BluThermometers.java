package cloud.cholewa.heating.thermometer;

import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.shelly.model.BthomeSensorConfig;
import cloud.cholewa.shelly.model.BthomeSensorStatus;
import cloud.cholewa.shelly.model.ShellyComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

//The BLU H&T thermometers heard by the BTHome gateways (HAS-364) - the way of the window sensors (WindowSensors), a
//sibling of it rather than a generalised map of components (two readers, the rule of three):
//- Which bthomesensor component of which gateway is the temperature (object 0x45) or the humidity (0x2E) of which
//  thermometer of the registry (Thermometers.bluRoomOf, by its address) is read from the gateways themselves -
//  Shelly.GetComponents, every five minutes, with the window sensors: their read hands over the components of every
//  gateway (WindowSensors.gatewayComponents), so no gateway is asked twice.
//- That read also gives the value each component holds, with when the gateway heard it: a BLU sensor publishes on a
//  change only, so after a start the first reading of a room comes from there, not from a packet. The gateway stamps
//  every packet (about one a minute), so the read brings a newer reading every five minutes even when nothing changed.
//- Between two reads the status topic of the component carries the value, on a change (ThermometerEventRouter)
@Slf4j
@Component
public class BluThermometers {

    private static final String SENSOR = "bthomesensor:";

    private final Thermometers thermometers;
    //a firmware adds fields to a config, and the models have only what is read here
    private final JsonMapper jsonMapper;
    //which component of which gateway is what of which room, as the last read of the gateways found it
    private final AtomicReference<Map<SensorComponent, Target>> components = new AtomicReference<>(Map.of());

    public BluThermometers(final Thermometers thermometers, final JsonMapper jsonMapper) {
        this.thermometers = thermometers;
        this.jsonMapper = jsonMapper.rebuild().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    }

    /** What a BLU H&T reports, by its BTHome object. */
    public enum Metric {
        TEMPERATURE(0x45), HUMIDITY(0x2E);

        private final int object;

        Metric(final int object) {
            this.object = object;
        }

        static Optional<Metric> of(final Integer object) {
            return Arrays.stream(values()).filter(metric -> Integer.valueOf(metric.object).equals(object)).findFirst();
        }
    }

    /** A bthomesensor component of a gateway, by the MQTT id of the gateway in lower case. */
    record SensorComponent(String gateway, int component) {
    }

    /** What a component is: the temperature or the humidity of the thermometer at this address, in this room. */
    public record Target(RoomName room, Metric metric, String address) {
    }

    /** A value a gateway holds for a thermometer, with when it heard it - null when it did not say. */
    public record Reading(RoomName room, Metric metric, double value, Instant heardAt) {
    }

    //the components of every gateway read, by its MQTT id: the map is built anew from them whole, so a gateway gone
    //from the registry, or a thermometer moved to another gateway, leaves no component behind. Answers the value of
    //every component found, for the readings of the rooms
    public List<Reading> apply(final Map<String, List<ShellyComponent>> gateways) {
        final Map<SensorComponent, Target> found = new HashMap<>();
        final List<Reading> readings = new ArrayList<>();
        gateways.forEach((gateway, list) -> list.stream()
            .filter(component -> component.getKey() != null && component.getKey().startsWith(SENSOR))
            .forEach(component -> as(component.getConfig(), BthomeSensorConfig.class)
                .filter(config -> config.getId() != null && config.getAddr() != null)
                .flatMap(config -> Metric.of(config.getObjId())
                    .flatMap(metric -> thermometers.bluRoomOf(config.getAddr())
                        .map(room -> new Target(room, metric, config.getAddr().toLowerCase(Locale.ROOT))))
                    .map(target -> {
                        found.put(new SensorComponent(gateway.toLowerCase(Locale.ROOT), config.getId()), target);
                        return target;
                    }))
                .ifPresent(target -> as(component.getStatus(), BthomeSensorStatus.class)
                    .flatMap(status -> reading(target, status))
                    .ifPresent(readings::add))));

        final Map<SensorComponent, Target> before = components.getAndSet(Map.copyOf(found));
        if (!before.equals(found)) {
            log.info("BLU thermometer components: {}", found);
        }
        return readings;
    }

    //is this status topic a value of a thermometer - asked before a body is read
    public Optional<Target> target(final String gatewayId, final Integer component) {
        if (gatewayId == null || component == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(components.get().get(new SensorComponent(gatewayId.toLowerCase(Locale.ROOT), component)));
    }

    //a value that is not a number is no reading: a sensor not heard since the gateway started holds null
    public static Optional<Reading> reading(final Target target, final BthomeSensorStatus status) {
        if (!(status.getValue() instanceof Number value)) {
            return Optional.empty();
        }
        final Instant heardAt = status.getLastUpdatedTs() == null || status.getLastUpdatedTs() <= 0
            ? null : Instant.ofEpochSecond(status.getLastUpdatedTs());
        return Optional.of(new Reading(target.room(), target.metric(), value.doubleValue(), heardAt));
    }

    private <T> Optional<T> as(final Map<String, Object> raw, final Class<T> model) {
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(jsonMapper.convertValue(raw, model));
        } catch (JacksonException | IllegalArgumentException notTheModel) {
            return Optional.empty();
        }
    }
}
