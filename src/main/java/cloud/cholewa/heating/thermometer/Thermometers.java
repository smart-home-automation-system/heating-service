package cloud.cholewa.heating.thermometer;

import cloud.cholewa.heating.registry.DeviceRegistryClient;
import cloud.cholewa.home.model.BthomeBinding;
import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.home.model.ShellyBinding;
import cloud.cholewa.home.model.SmartDevice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

//The Shelly thermometers of the rooms: the switched-on devices of the kind temperature sensor in the registry, read
//with the window sensors every five minutes; a read that fails keeps the last ones.
//- A Wi-Fi thermometer (HAS-360) has a Shelly binding with its MQTT id - the Plus H&T of sanctum.
//- A BLU H&T (HAS-364) has the bthome binding, its Bluetooth address: it is read through the gateway that hears it
//  (BluThermometers) - the bathrooms.
//- A room with an Eaton room sensor keeps the Eaton reading (one reading a room, the stored history stays one
//  series) - a switched-off Eaton sensor too, amx-service publishes it all the same. Its Wi-Fi thermometer is not
//  read at all; its BLU thermometer is mapped (the log shows it), its readings are dropped (accept).
//- Two thermometers in one room: the newer reading counts (accept), whichever device brought it
@Slf4j
@Component
@RequiredArgsConstructor
public class Thermometers {

    private final DeviceRegistryClient deviceRegistryClient;
    private final AtomicReference<Known> known = new AtomicReference<>(Known.NONE);
    //when the reading last taken into each room was measured: an older or the same one is not taken again
    private final Map<RoomName, LocalDateTime> lastTaken = new ConcurrentHashMap<>();

    /**
     * The thermometers of the registry: the Wi-Fi ones by their MQTT id (the rooms without an Eaton sensor only),
     * the BLU ones by their address in lower case (every room) and the rooms that keep their Eaton sensor.
     */
    record Known(Map<String, RoomName> wifi, Map<String, RoomName> blu, Set<RoomName> eatonRooms) {
        static final Known NONE = new Known(Map.of(), Map.of(), Set.of());
    }

    //the room of the Wi-Fi thermometer with this MQTT id, when it is one this service reads
    public Optional<RoomName> roomOf(final String deviceId) {
        return Optional.ofNullable(deviceId).map(id -> known.get().wifi().get(id));
    }

    //the room of the BLU thermometer with this Bluetooth address
    public Optional<RoomName> bluRoomOf(final String address) {
        return Optional.ofNullable(address).map(value -> known.get().blu().get(value.toLowerCase(Locale.ROOT)));
    }

    /**
     * Whether a reading of a Shelly thermometer, measured at this time, is taken into the room: never in a room with
     * an Eaton room sensor, and only when it is newer than the last one taken there - the same reading read again from
     * its gateway, or an older one of a second thermometer, is not. Before the first one since the start, the reading
     * the room already holds counts ({@code held} - the last stored one, seeded at the start), so a rollout does not
     * store the reading a gateway still holds a second time.
     */
    public boolean accept(final RoomName room, final LocalDateTime measuredAt, final LocalDateTime held) {
        if (room == null || measuredAt == null || !reads(room)) {
            return false;
        }
        final AtomicBoolean taken = new AtomicBoolean();
        lastTaken.compute(room, (key, last) -> {
            final LocalDateTime newest = last != null ? last : held;
            if (newest != null && !measuredAt.isAfter(newest)) {
                return last;
            }
            taken.set(true);
            return measuredAt;
        });
        return taken.get();
    }

    //whether the readings of a Shelly thermometer count in the room: not where an Eaton room sensor is
    public boolean reads(final RoomName room) {
        return !known.get().eatonRooms().contains(room);
    }

    public Mono<Void> refresh() {
        return Mono.defer(deviceRegistryClient::getTemperatureSensors)
            .map(Thermometers::read)
            .doOnNext(read -> {
                final Known before = known.getAndSet(read);
                if (!before.equals(read)) {
                    log.info("Shelly thermometers read: Wi-Fi {}, BLU {}; rooms on their Eaton sensor: {}",
                        read.wifi(), read.blu(), read.eatonRooms());
                }
            })
            .then()
            .onErrorResume(failure -> Mono.fromRunnable(() -> log.warn(
                "The thermometers could not be read from the registry, the last ones stay: {}", failure.toString())));
    }

    static Known read(final List<SmartDevice> sensors) {
        final Set<RoomName> eatonRooms = sensors.stream()
            .filter(device -> device.getEaton() != null && device.getRoom() != null)
            .map(SmartDevice::getRoom)
            .collect(Collectors.toUnmodifiableSet());
        final List<SmartDevice> shelly = sensors.stream()
            .filter(device -> Boolean.TRUE.equals(device.getEnabled()) && device.getRoom() != null)
            .toList();
        final Map<String, RoomName> wifi = shelly.stream()
            .filter(device -> !eatonRooms.contains(device.getRoom()))
            .filter(device -> Optional.ofNullable(device.getShelly()).map(ShellyBinding::getDeviceId).isPresent())
            .collect(Collectors.toUnmodifiableMap(device -> device.getShelly().getDeviceId(), SmartDevice::getRoom,
                (one, other) -> one));
        final Map<String, RoomName> blu = shelly.stream()
            .filter(device -> Optional.ofNullable(device.getBthome()).map(BthomeBinding::getAddress).isPresent())
            .collect(Collectors.toUnmodifiableMap(device -> device.getBthome().getAddress().toLowerCase(Locale.ROOT),
                SmartDevice::getRoom, (one, other) -> one));
        return new Known(wifi, blu, eatonRooms);
    }
}
