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
import java.util.ArrayList;
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
//- The last reading of every sensor is stored (HAS-419, WindowSensorStore) and read back at the start as not
//  confirmed: the history a gateway value is compared with, never a state that holds the heating. On 2026-11-10 a
//  grid outage restarted the gateways with this service, the gateways restored the "open" half of short open/close
//  pairs with its old time, and nothing here remembered the newer "closed"
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
    private final WindowSensorStore store;
    private final Clock clock;

    //the opening sensors of the registry by their id, as last read
    private final AtomicReference<Map<Long, Sensor>> sensors = new AtomicReference<>(Map.of());
    //the BTHome gateways of the registry, as last read
    private final AtomicReference<List<Gateway>> gateways = new AtomicReference<>(List.of());
    //which component of which gateway is the window of which sensor
    private final Map<SensorComponent, Long> components = new ConcurrentHashMap<>();
    private final Map<Long, Reading> readings = new ConcurrentHashMap<>();
    //when the registry first listed a sensor switched on: one that never reports is silent from then on
    private final Map<Long, LocalDateTime> firstListed = new ConcurrentHashMap<>();
    //what every gateway answered at its last read, for the BLU thermometers (HAS-364): one read of a gateway serves both
    private final Map<String, List<ShellyComponent>> gatewayComponents = new ConcurrentHashMap<>();
    //the gateways the last read failed on: a WARN when one starts failing, an INFO when it is back
    private final Set<String> failing = ConcurrentHashMap.newKeySet();
    //the sensors a gateway gave an older value of since the start: an INFO once per sensor (HAS-419)
    private final Set<Long> olderTold = ConcurrentHashMap.newKeySet();
    //the sensors the stored readings were last trimmed to: the table is trimmed only when the registry lists others
    private final AtomicReference<Set<Long>> trimmedTo = new AtomicReference<>(Set.of());

    public WindowSensors(
        final DeviceRegistryClient deviceRegistryClient,
        final ShellyRpcClient shellyRpcClient,
        final JsonMapper jsonMapper,
        final WindowProperties properties,
        final WindowSensorStore store,
        final Clock clock
    ) {
        this.deviceRegistryClient = deviceRegistryClient;
        this.shellyRpcClient = shellyRpcClient;
        this.jsonMapper = jsonMapper.rebuild().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
        this.properties = properties;
        this.store = store;
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
     * gateway heard it; {@code confirmedAt} when a gateway last stood for it, null for a reading read back
     * from the table that no gateway has stood for since the start (HAS-419); {@code openSince} when it
     * opened, while it is open.
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

    //The stored readings, before the first read of the gateways (HeatingScheduleLoader): each one not confirmed, so not
    //known - it is only what a gateway value is compared with. Never fails; a reading already in memory is kept
    public Mono<Void> restore() {
        return store.load()
            .filter(stored -> readings.putIfAbsent(stored.sensorId(), stored.reading()) == null)
            .count()
            .doOnNext(count -> log.info("{} stored window sensor reading(s) restored, not known until a gateway stands for them", count))
            .then();
    }

    //Never fails. A registry away keeps the last sensors and gateways, and the gateways are read all
    //the same - a database-service down for an hour must not make every window unknown; a gateway
    //away keeps its map and lets its sensors age until they are unknown
    public Mono<Void> refresh() {
        return Mono.defer(() -> Mono.zip(deviceRegistryClient.getOpeningSensors(), deviceRegistryClient.getBthomeGateways()))
            .flatMap(read -> {
                final Map<Long, Sensor> listedSensors = toSensors(read.getT1());
                sensors.set(listedSensors);
                final LocalDateTime now = now();
                //a sensor switched on later is given its time from then, not from when it was registered
                final Set<Long> enabled = listedSensors.values().stream()
                    .filter(Sensor::enabled)
                    .map(Sensor::id)
                    .collect(Collectors.toSet());
                enabled.forEach(id -> firstListed.putIfAbsent(id, now));
                firstListed.keySet().retainAll(enabled);
                readings.keySet().retainAll(listedSensors.keySet());

                final List<Gateway> listedNow = toGateways(read.getT2());
                gateways.set(listedNow);
                //a gateway the registry no longer lists is read no more: its components go with it
                final Set<String> listed = listedNow.stream().map(Gateway::deviceId).collect(Collectors.toSet());
                components.keySet().removeIf(key -> !listed.contains(key.gateway()));
                gatewayComponents.keySet().retainAll(listed);
                failing.retainAll(listed);
                return forgetTheGone(listedSensors.keySet()).thenReturn(listedNow);
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
            .map(list -> {
                final List<Change> changes = apply(gateway, list);
                gatewayComponents.put(gateway.deviceId(), List.copyOf(list));
                if (failing.remove(gateway.deviceId())) {
                    log.info("Gateway {} ({}) is read again", gateway.id(), gateway.name());
                }
                return changes;
            })
            .doOnError(e -> {
                if (failing.add(gateway.deviceId())) {
                    log.warn("Gateway {} ({}) could not be read, its window sensors are unknown once it has been silent for {}: {}",
                        gateway.id(), gateway.name(), properties.gatewaySilentAfter(), e.toString());
                }
            })
            .onErrorComplete()
            //the changes stored one after the other, each never failing: a write never stops the read of the next gateway
            .flatMapMany(Flux::fromIterable)
            .concatMap(this::storeChange)
            .then();
    }

    //the stored readings of the sensors gone from the registry go with them, once the registry lists others; never
    //fails. A registry that lists none keeps them: one empty answer must not wipe what a restart compares with
    private Mono<Void> forgetTheGone(final Set<Long> listed) {
        return Mono.just(listed)
            .filter(ids -> !ids.isEmpty() && !ids.equals(trimmedTo.getAndSet(ids)))
            .flatMap(store::retain);
    }

    /** A reading of a sensor that changed, to be stored. */
    record Change(long sensor, Reading reading) {
    }

    private Mono<Void> storeChange(final Change change) {
        return store.save(change.sensor(), change.reading());
    }

    //the window components of the gateway that belong to a sensor of the registry, each with what it
    //holds now; a component of another device, or of no window, is not this service's. Answers the
    //readings that changed, for the table
    List<Change> apply(final Gateway gateway, final List<ShellyComponent> list) {
        final Map<String, Sensor> byAddress = sensors.get().values().stream()
            .filter(sensor -> sensor.address() != null)
            .collect(Collectors.toMap(sensor -> sensor.address().toLowerCase(Locale.ROOT), sensor -> sensor, (a, b) -> a));
        final LocalDateTime now = now();
        final Map<SensorComponent, Long> found = new HashMap<>();
        final List<Change> changes = new ArrayList<>();

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
                        status -> report(gateway.deviceId(), sensor.id(), status, now).ifPresent(changes::add),
                        () -> confirm(sensor.id(), now))));

        components.keySet().removeIf(key -> key.gateway().equals(gateway.deviceId()) && !found.containsKey(key));
        components.putAll(found);
        return changes;
    }

    //the components every gateway of the registry answered at its last read, by its MQTT id in lower case - a gateway
    //that failed keeps what it answered before (its values carry their time, so nothing old passes for new)
    public Map<String, List<ShellyComponent>> gatewayComponents() {
        return Map.copyOf(gatewayComponents);
    }

    //is this status topic the window of a sensor - asked before a body is read
    public boolean knows(final String gatewayId, final Integer component) {
        return gatewayId != null && component != null
            && components.containsKey(new SensorComponent(gatewayId.toLowerCase(Locale.ROOT), component));
    }

    //the reading is taken at once; what the answer stores runs when it is subscribed to, and never fails - a failed
    //write never stops the listener
    public Mono<Void> observe(final String gatewayId, final int component, final BthomeSensorStatus status) {
        final String gateway = gatewayId.toLowerCase(Locale.ROOT);
        return Optional.ofNullable(components.get(new SensorComponent(gateway, component)))
            .flatMap(sensor -> report(gateway, sensor, status, now()))
            .map(this::storeChange)
            .orElseGet(Mono::empty);
    }

    //A value that is not a truth is not a window: the gateway stood for the sensor all the same.
    //Of two reports the one the gateway heard later counts - a read of the gateway may bring an older
    //state than a publish that came a moment before it, and a gateway restarted may bring back a state
    //older than the one stored before the restart (HAS-419). A window that opens is open since the gateway
    //heard it, and since now when that time is not given or is ahead of the clock here.
    //Answers the reading to store: a new value, or the same value heard later - never a confirmation
    private Optional<Change> report(final String gateway, final long sensor, final BthomeSensorStatus status, final LocalDateTime now) {
        final Boolean open = status.getValue() instanceof Boolean value ? value : null;
        final Instant updatedAt = status.getLastUpdatedTs() == null ? null : Instant.ofEpochSecond(status.getLastUpdatedTs());
        final AtomicReference<Change> changed = new AtomicReference<>();
        readings.compute(sensor, (id, held) -> {
            if (open == null) {
                return held == null ? new Reading(null, null, now, null) : confirmedIfHeard(held, now);
            }
            if (held != null && held.updatedAt() != null && updatedAt != null && updatedAt.isBefore(held.updatedAt())) {
                tellOlder(gateway, sensor, open, updatedAt, held);
                return confirmed(held, now);
            }
            final Reading next = next(sensor, held, open, updatedAt, now);
            if (isChange(held, next)) {
                changed.set(new Change(sensor, next));
            }
            return next;
        });
        return Optional.ofNullable(changed.get());
    }

    //a value not older than the one held: the same value keeps its opening, another one is logged and starts anew
    private Reading next(final long sensor, final Reading held, final boolean open, final Instant updatedAt, final LocalDateTime now) {
        if (held != null && Boolean.valueOf(open).equals(held.open())) {
            return new Reading(open, updatedAt, now, held.openSince());
        }
        log.info("Window sensor {} ({}): {}", sensor, nameOf(sensor), word(open));
        return new Reading(open, updatedAt, now, open ? openedAt(updatedAt, now) : null);
    }

    //a new value, or the same value heard later - never a confirmation of what is held
    private static boolean isChange(final Reading held, final Reading next) {
        return held == null || !Objects.equals(held.open(), next.open())
            || next.updatedAt() != null && !next.updatedAt().equals(held.updatedAt());
    }

    private static String word(final Boolean open) {
        return Boolean.TRUE.equals(open) ? "open" : "closed";
    }

    //once per sensor and start, so that the next restored state of a gateway shows in the logs
    private void tellOlder(final String gateway, final long sensor, final boolean open, final Instant updatedAt, final Reading held) {
        if (olderTold.add(sensor)) {
            log.info("Window sensor {} ({}): gateway {} gave {} heard at {}, older than the kept {} heard at {} - the kept one stays",
                sensor, nameOf(sensor), gateway, word(open), heardAt(updatedAt), word(held.open()), heardAt(held.updatedAt()));
        }
    }

    private LocalDateTime heardAt(final Instant instant) {
        return LocalDateTime.ofInstant(instant, clock.getZone());
    }

    //a component without a window value: the gateway stands for what it heard before, but a reading read back from the
    //table it never stood for stays not known - only a value of the gateway confirms that one (HAS-419)
    private void confirm(final long sensor, final LocalDateTime now) {
        readings.compute(sensor, (id, held) -> held == null ? new Reading(null, null, now, null) : confirmedIfHeard(held, now));
    }

    private static Reading confirmedIfHeard(final Reading held, final LocalDateTime now) {
        return held.confirmedAt() == null ? held : confirmed(held, now);
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

    /**
     * A switched-on sensor whose gateway stands for "open" now (HAS-350).
     *
     * @param openSince since when it reads open
     * @param stateAt   when its gateway last heard the window value it holds - the window component's
     *                  {@code last_updated_ts}, never later than now; {@code openSince} when the gateway gave
     *                  none. Never the time of the last packet of the device: its battery beacons keep coming
     *                  while the window value is frozen
     */
    public record OpenSensor(long id, String name, RoomName room, LocalDateTime openSince, LocalDateTime stateAt) {
    }

    /** What a gateway stands for now of a sensor: open or closed, and when that value was heard (HAS-350). */
    public record WindowValue(boolean open, LocalDateTime stateAt) {
    }

    //the switched-on sensors of these rooms that read open now, by id
    public List<OpenSensor> open(final Set<RoomName> rooms, final LocalDateTime now) {
        return sensors.get().values().stream()
            .filter(sensor -> sensor.enabled() && rooms.contains(sensor.room()))
            .flatMap(sensor -> valueOf(sensor.id(), now)
                .filter(WindowValue::open)
                .map(value -> new OpenSensor(sensor.id(), sensor.name(), sensor.room(),
                    readings.get(sensor.id()).openSince(), value.stateAt()))
                .stream())
            .sorted(Comparator.comparingLong(OpenSensor::id))
            .toList();
    }

    //the value a gateway stands for now, empty while the sensor is not known
    public Optional<WindowValue> valueOf(final long sensor, final LocalDateTime now) {
        return Optional.ofNullable(readings.get(sensor))
            .filter(reading -> isKnown(reading, now))
            .map(reading -> new WindowValue(reading.open(), stateAt(reading, now)));
    }

    private LocalDateTime stateAt(final Reading reading, final LocalDateTime now) {
        if (reading.updatedAt() == null) {
            return reading.openSince() != null ? reading.openSince() : reading.confirmedAt();
        }
        final LocalDateTime heard = LocalDateTime.ofInstant(reading.updatedAt(), clock.getZone());
        return heard.isAfter(now) ? now : heard;
    }

    //a switched-on sensor of the registry, as last read
    Optional<Sensor> describe(final long sensor) {
        return Optional.ofNullable(sensors.get().get(sensor)).filter(Sensor::enabled);
    }

    //whether a gateway stands for a state of the sensor now
    boolean isKnown(final long sensor, final LocalDateTime now) {
        return isKnown(readings.get(sensor), now);
    }

    //a reading read back from the table is not known until a gateway stands for it (HAS-419)
    private boolean isKnown(final Reading reading, final LocalDateTime now) {
        return reading != null && reading.open() != null && reading.confirmedAt() != null
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
