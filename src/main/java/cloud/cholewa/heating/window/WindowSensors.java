package cloud.cholewa.heating.window;

import cloud.cholewa.heating.config.WindowProperties;
import cloud.cholewa.heating.registry.DeviceRegistryClient;
import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.home.model.SmartDevice;
import cloud.cholewa.shelly.http.ShellyRpcClient;
import cloud.cholewa.shelly.model.BthomeSensorConfig;
import cloud.cholewa.shelly.model.BthomeSensorStatus;
import cloud.cholewa.shelly.model.ShellyComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

//The window and door sensors of the rooms (HAS-232): Shelly BLU Door/Window sensors adopted by BTHome
//gateways, each a device of the kind opening sensor in the registry, in the room of its window or
//door. The reading is a copy of WindowSensors of blinds-service (HAS-290) - two services, two copies,
//the commons rule - judged per room instead of per blind:
//- Which bthomesensor component of which gateway is the window (object 0x2D) of which sensor is read
//  from the gateways themselves - Shelly.GetComponents, every five minutes - with the state each one
//  holds. One gateway per sensor is the rule of the house; should two deliver one sensor, the newer
//  report wins.
//- Between two reads the state comes from the status topic of the component, about once a change.
//- A sensor sends only when it changes (no beacon), so its own silence proves nothing: a state stands
//  while its gateway confirms it, by a read or a publish, within heating.window.gateway-silent-after.
//  A sensor that never reported, or whose gateway went silent, is unknown.
//- A room is open when one of its sensors reports open, unknown when one is not known and none is
//  open, closed when every one is closed; a room without a sensor heats as it always did. A
//  switched-off sensor is left out of its room, as if it were not there.
//- The heating of a room is held once a window or a door of it has stood open for
//  heating.window.hold-after (owner: three minutes); an unknown sensor never holds it
@Slf4j
@Component
public class WindowSensors {

    //the BTHome object of a window or a door: true is open
    static final int WINDOW_OBJECT = 0x2D;
    private static final String SENSOR = "bthomesensor:";

    private final DeviceRegistryClient deviceRegistryClient;
    private final ShellyRpcClient shellyRpcClient;
    //a firmware adds fields to a config, and the models have only what is read here
    private final JsonMapper jsonMapper;
    private final WindowProperties properties;
    private final Clock clock;

    //the opening sensors of the registry by their id, as last read
    private final AtomicReference<Map<Long, Sensor>> sensors = new AtomicReference<>(Map.of());
    //the BTHome gateways of the registry, as last read
    private final AtomicReference<List<Gateway>> gateways = new AtomicReference<>(List.of());
    //which component of which gateway is the window of which sensor
    private final Map<SensorComponent, Long> components = new ConcurrentHashMap<>();
    private final Map<Long, Reading> readings = new ConcurrentHashMap<>();
    //when the registry first listed a sensor: one that never reports is silent from then on
    private final Map<Long, LocalDateTime> firstListed = new ConcurrentHashMap<>();
    //the gateways the last read failed on: a WARN when one starts failing, an INFO when it is back
    private final Set<String> failing = ConcurrentHashMap.newKeySet();

    public WindowSensors(
        final DeviceRegistryClient deviceRegistryClient,
        final ShellyRpcClient shellyRpcClient,
        final JsonMapper jsonMapper,
        final WindowProperties properties,
        final Clock clock
    ) {
        this.deviceRegistryClient = deviceRegistryClient;
        this.shellyRpcClient = shellyRpcClient;
        this.jsonMapper = jsonMapper.rebuild().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
        this.properties = properties;
        this.clock = clock;
    }

    /** An opening sensor of the registry: its Bluetooth address, null for one that is no BTHome sensor. */
    record Sensor(long id, String name, RoomName room, String address, boolean enabled) {
    }

    /** A bthomesensor component of a gateway, by the MQTT id of the gateway in lower case. */
    record SensorComponent(String gateway, int component) {
    }

    /**
     * What a sensor last said: {@code open} null until it said anything; {@code updatedAt} when the
     * gateway heard it; {@code confirmedAt} when a gateway last stood for it; {@code openSince} when
     * it opened, while it is open.
     */
    record Reading(Boolean open, Instant updatedAt, LocalDateTime confirmedAt, LocalDateTime openSince) {
    }

    record Gateway(long id, String name, String address, String deviceId) {
    }

    /**
     * A switched-on sensor whose state is not known, for as long as the gateways are given to confirm
     * one at least.
     *
     * @param lastStateAt when a gateway last stood for a state of it, null when none ever did since
     *                    the start
     */
    public record SilentSensor(long id, String name, RoomName room, LocalDateTime lastStateAt) {
    }

    //Never fails. A registry away keeps the last sensors and gateways, and the gateways are read all
    //the same - a database-service down for an hour must not make every window unknown; a gateway
    //away keeps its map and lets its sensors age until they are unknown
    public Mono<Void> refresh() {
        return Mono.defer(() -> Mono.zip(deviceRegistryClient.getOpeningSensors(), deviceRegistryClient.getBthomeGateways()))
            .map(read -> {
                final Map<Long, Sensor> listedSensors = toSensors(read.getT1());
                sensors.set(listedSensors);
                final LocalDateTime now = now();
                listedSensors.keySet().forEach(id -> firstListed.putIfAbsent(id, now));
                firstListed.keySet().retainAll(listedSensors.keySet());
                readings.keySet().retainAll(listedSensors.keySet());

                final List<Gateway> listedNow = toGateways(read.getT2());
                gateways.set(listedNow);
                //a gateway the registry no longer lists is read no more: its components go with it
                final Set<String> listed = listedNow.stream().map(Gateway::deviceId).collect(Collectors.toSet());
                components.keySet().removeIf(key -> !listed.contains(key.gateway()));
                failing.retainAll(listed);
                return listedNow;
            })
            .onErrorResume(e -> {
                log.warn("The window sensors could not be read from the registry, the last ones stay: {}", e.toString());
                return Mono.just(gateways.get());
            })
            .flatMapMany(Flux::fromIterable)
            .concatMap(this::read)
            .then();
    }

    private Mono<Void> read(final Gateway gateway) {
        return shellyRpcClient.dynamicComponents(gateway.address())
            .doOnNext(list -> {
                apply(gateway, list);
                if (failing.remove(gateway.deviceId())) {
                    log.info("Gateway {} ({}) is read again", gateway.id(), gateway.name());
                }
            })
            .doOnError(e -> {
                if (failing.add(gateway.deviceId())) {
                    log.warn("Gateway {} ({}) could not be read, its window sensors are unknown once it has been silent for {}: {}",
                        gateway.id(), gateway.name(), properties.gatewaySilentAfter(), e.toString());
                }
            })
            .onErrorComplete()
            .then();
    }

    //the window components of the gateway that belong to a sensor of the registry, each with what it
    //holds now; a component of another device, or of no window, is not this service's
    void apply(final Gateway gateway, final List<ShellyComponent> list) {
        final Map<String, Sensor> byAddress = sensors.get().values().stream()
            .filter(sensor -> sensor.address() != null)
            .collect(Collectors.toMap(sensor -> sensor.address().toLowerCase(Locale.ROOT), sensor -> sensor, (a, b) -> a));
        final LocalDateTime now = now();
        final Map<SensorComponent, Long> found = new HashMap<>();

        list.stream()
            .filter(component -> component.getKey() != null && component.getKey().startsWith(SENSOR))
            .forEach(component -> as(component.getConfig(), BthomeSensorConfig.class)
                .filter(config -> config.getId() != null && Integer.valueOf(WINDOW_OBJECT).equals(config.getObjId()))
                .flatMap(config -> Optional.ofNullable(config.getAddr())
                    .map(address -> byAddress.get(address.toLowerCase(Locale.ROOT)))
                    .map(sensor -> {
                        found.put(new SensorComponent(gateway.deviceId(), config.getId()), sensor.id());
                        return sensor;
                    }))
                .ifPresent(sensor -> as(component.getStatus(), BthomeSensorStatus.class)
                    .ifPresentOrElse(
                        status -> report(sensor.id(), status, now),
                        () -> confirm(sensor.id(), now))));

        components.keySet().removeIf(key -> key.gateway().equals(gateway.deviceId()) && !found.containsKey(key));
        components.putAll(found);
    }

    //is this status topic the window of a sensor - asked before a body is read
    public boolean knows(final String gatewayId, final Integer component) {
        return gatewayId != null && component != null
            && components.containsKey(new SensorComponent(gatewayId.toLowerCase(Locale.ROOT), component));
    }

    public void observe(final String gatewayId, final int component, final BthomeSensorStatus status) {
        final Long sensor = components.get(new SensorComponent(gatewayId.toLowerCase(Locale.ROOT), component));
        if (sensor != null) {
            report(sensor, status, now());
        }
    }

    //A value that is not a truth is not a window: the gateway stood for the sensor all the same.
    //Of two reports the one the gateway heard later counts - a read of the gateway may bring an older
    //state than a publish that came a moment before it. A window that opens is open since the gateway
    //heard it, and since now when that time is not given or is ahead of the clock here
    private void report(final long sensor, final BthomeSensorStatus status, final LocalDateTime now) {
        final Boolean open = status.getValue() instanceof Boolean value ? value : null;
        final Instant updatedAt = status.getLastUpdatedTs() == null ? null : Instant.ofEpochSecond(status.getLastUpdatedTs());
        readings.compute(sensor, (id, held) -> {
            if (open == null) {
                return held == null ? new Reading(null, null, now, null) : confirmed(held, now);
            }
            if (held != null && held.updatedAt() != null && updatedAt != null && updatedAt.isBefore(held.updatedAt())) {
                return confirmed(held, now);
            }
            if (held != null && open.equals(held.open())) {
                return new Reading(open, updatedAt, now, held.openSince());
            }
            log.info("Window sensor {} ({}): {}", sensor, nameOf(sensor), open ? "open" : "closed");
            return new Reading(open, updatedAt, now, open ? openedAt(updatedAt, now) : null);
        });
    }

    private void confirm(final long sensor, final LocalDateTime now) {
        readings.compute(sensor, (id, held) -> held == null ? new Reading(null, null, now, null) : confirmed(held, now));
    }

    private static Reading confirmed(final Reading held, final LocalDateTime now) {
        return new Reading(held.open(), held.updatedAt(), now, held.openSince());
    }

    private LocalDateTime openedAt(final Instant updatedAt, final LocalDateTime now) {
        if (updatedAt == null) {
            return now;
        }
        final LocalDateTime heard = LocalDateTime.ofInstant(updatedAt, clock.getZone());
        return heard.isAfter(now) ? now : heard;
    }

    public RoomWindow windowOf(final RoomName room) {
        return windowOf(room, now());
    }

    public RoomWindow windowOf(final RoomName room, final LocalDateTime now) {
        final List<Long> ids = sensors.get().values().stream()
            .filter(sensor -> sensor.enabled() && sensor.room() == room)
            .map(Sensor::id)
            .toList();
        if (ids.isEmpty()) {
            return RoomWindow.NO_SENSOR;
        }

        final List<Reading> known = ids.stream()
            .map(readings::get)
            .filter(reading -> isKnown(reading, now))
            .toList();
        final Optional<LocalDateTime> openSince = known.stream()
            .filter(reading -> reading.open())
            .map(Reading::openSince)
            .min(Comparator.naturalOrder());
        if (openSince.isPresent()) {
            final boolean hold = !openSince.get().plus(properties.holdAfter()).isAfter(now);
            return new RoomWindow(WindowState.OPEN, openSince.get(), hold);
        }
        return new RoomWindow(known.size() < ids.size() ? WindowState.UNKNOWN : WindowState.CLOSED, null, false);
    }

    //the switched-on sensors of these rooms that have had no known state for gateway-silent-after:
    //since the gateway last stood for a state of one, or since the registry first listed it
    public List<SilentSensor> silent(final Set<RoomName> rooms, final LocalDateTime now) {
        final LocalDateTime oldest = now.minus(properties.gatewaySilentAfter());
        return sensors.get().values().stream()
            .filter(sensor -> sensor.enabled() && rooms.contains(sensor.room()))
            .filter(sensor -> !isKnown(readings.get(sensor.id()), now))
            .map(sensor -> {
                final Reading reading = readings.get(sensor.id());
                final LocalDateTime lastStateAt = reading == null || reading.open() == null ? null : reading.confirmedAt();
                final LocalDateTime since = lastStateAt != null ? lastStateAt : firstListed.getOrDefault(sensor.id(), now);
                return !since.isBefore(oldest) ? null : new SilentSensor(sensor.id(), sensor.name(), sensor.room(), lastStateAt);
            })
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingLong(SilentSensor::id))
            .toList();
    }

    //a switched-on sensor of the registry, as last read
    Optional<Sensor> describe(final long sensor) {
        return Optional.ofNullable(sensors.get().get(sensor)).filter(Sensor::enabled);
    }

    private boolean isKnown(final Reading reading, final LocalDateTime now) {
        return reading != null && reading.open() != null
            && !reading.confirmedAt().isBefore(now.minus(properties.gatewaySilentAfter()));
    }

    private String nameOf(final long sensor) {
        final Sensor known = sensors.get().get(sensor);
        return known == null ? "?" : known.name();
    }

    //a sensor without a room belongs to no room's heating, and is left out
    private static Map<Long, Sensor> toSensors(final List<SmartDevice> devices) {
        final Map<Long, Sensor> read = new HashMap<>();
        devices.stream()
            .filter(Objects::nonNull)
            .filter(device -> device.getId() != null && device.getRoom() != null)
            .forEach(device -> read.put(device.getId(), new Sensor(
                device.getId(),
                device.getName(),
                device.getRoom(),
                device.getBthome() == null ? null : device.getBthome().getAddress(),
                Boolean.TRUE.equals(device.getEnabled()))));
        return Map.copyOf(read);
    }

    //a gateway is a Shelly on the network with its MQTT id; one without either cannot be read
    private static List<Gateway> toGateways(final List<SmartDevice> devices) {
        return devices.stream()
            .filter(Objects::nonNull)
            .filter(device -> {
                final boolean usable = device.getId() != null && device.getShelly() != null
                    && device.getShelly().getAddress() != null && device.getShelly().getDeviceId() != null;
                if (!usable) {
                    log.warn("BTHome gateway {} ({}) of the registry has no address or MQTT id and is not read",
                        device.getId(), device.getName());
                }
                return usable;
            })
            .map(device -> new Gateway(device.getId(), device.getName(), device.getShelly().getAddress(),
                device.getShelly().getDeviceId().toLowerCase(Locale.ROOT)))
            .toList();
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

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
