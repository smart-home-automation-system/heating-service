package cloud.cholewa.heating.thermometer;

import cloud.cholewa.heating.registry.DeviceRegistryClient;
import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.home.model.ShellyBinding;
import cloud.cholewa.home.model.SmartDevice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

//The Wi-Fi Shelly thermometers of the rooms (HAS-360): the switched-on devices of the kind temperature sensor in the
//registry with a Shelly binding (a deviceId), each in its room - the Plus H&T of sanctum today. Read with the window
//sensors every five minutes; a read that fails keeps the last ones.
//- A room with an Eaton room sensor keeps the Eaton reading: its Shelly thermometer is not read (one reading a room,
//  the stored history stays one series) - a switched-off Eaton sensor too, amx-service publishes it all the same.
//- A temperature sensor with the bthome binding (a BLU H&T heard by a gateway, no deviceId) is skipped without a word:
//  it is read through its gateway (HAS-364).
//- Two thermometers in one room: the first by registry id is read, the other logged at WARN
@Slf4j
@Component
@RequiredArgsConstructor
public class Thermometers {

    private final DeviceRegistryClient deviceRegistryClient;
    private final AtomicReference<Map<String, RoomName>> known = new AtomicReference<>(Map.of());

    //the room of the thermometer with this MQTT id, when it is one this service reads
    public Optional<RoomName> roomOf(final String deviceId) {
        return Optional.ofNullable(deviceId).map(id -> known.get().get(id));
    }

    public Mono<Void> refresh() {
        return Mono.defer(deviceRegistryClient::getTemperatureSensors)
            .map(Thermometers::read)
            .doOnNext(read -> {
                final Map<String, RoomName> before = known.getAndSet(read);
                if (!before.equals(read)) {
                    log.info("Shelly thermometers read: {}", read);
                }
            })
            .then()
            .onErrorResume(failure -> Mono.fromRunnable(() -> log.warn(
                "The thermometers could not be read from the registry, the last ones stay: {}", failure.toString())));
    }

    //the Shelly thermometers by their MQTT id, in the rooms without an Eaton room sensor
    static Map<String, RoomName> read(final List<SmartDevice> sensors) {
        final Set<RoomName> eatonRooms = sensors.stream()
            .filter(device -> device.getEaton() != null && device.getRoom() != null)
            .map(SmartDevice::getRoom)
            .collect(Collectors.toSet());
        final List<SmartDevice> shelly = sensors.stream()
            .filter(device -> Boolean.TRUE.equals(device.getEnabled()) && device.getRoom() != null)
            .filter(device -> Optional.ofNullable(device.getShelly()).map(ShellyBinding::getDeviceId).isPresent())
            .filter(device -> !eatonRooms.contains(device.getRoom()))
            .sorted((one, other) -> Long.compare(Objects.requireNonNullElse(one.getId(), Long.MAX_VALUE),
                Objects.requireNonNullElse(other.getId(), Long.MAX_VALUE)))
            .toList();
        final Map<RoomName, SmartDevice> firstOfRoom = shelly.stream()
            .collect(Collectors.toMap(SmartDevice::getRoom, Function.identity(), (first, second) -> {
                log.warn("Two Shelly thermometers in room {}: {} is read, {} is not", first.getRoom().getValue(),
                    first.getId(), second.getId());
                return first;
            }));
        return firstOfRoom.values().stream()
            .collect(Collectors.toUnmodifiableMap(device -> device.getShelly().getDeviceId(), SmartDevice::getRoom));
    }
}
