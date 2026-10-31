package cloud.cholewa.heating.service;

import cloud.cholewa.heating.config.RelayConfig;
import cloud.cholewa.heating.db.model.HeatingScheduleEntity;
import cloud.cholewa.heating.db.repository.HeatingScheduleRepository;
import cloud.cholewa.heating.infrastructure.error.RoomNotFoundException;
import cloud.cholewa.heating.infrastructure.error.ScheduleException;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.Schedule;
import cloud.cholewa.heating.model.ScheduleBody;
import cloud.cholewa.heating.model.StoredSchedule;
import cloud.cholewa.home.model.RoomName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The heating schedules of the heaters, stored in {@code heating_schedule} (HAS-355). They are read whole
 * into the heaters of {@link Home} - at the start, and after every write (HAS-356) - and the control loop
 * keeps reading them from memory, never from the database at a reading.
 * <p>
 * A row this version cannot place is logged and left out, the others are loaded: a room or a heater the
 * house does not have or this service does not drive (SANCTUM), a name or a day it does not know. A heater
 * without a row has no schedule and is never in one.
 * <p>
 * A write (HAS-356) is checked by {@link ScheduleRules}, stored, and the schedules are loaded again before it
 * answers, so the next pass of the room - within a minute - heats by it. A load that fails after the row was
 * stored fails the write (the caller reads the rooms again); the heaters keep what they had until the next
 * write or a restart. The writes go one after the other: two at once could each pass the overlap check and
 * meet. That holds in one pod; during a rolling deploy two pods are not written to at once (one writer - the
 * dashboard or Bruno - at a time).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeatingScheduleService {

    private final Home home;
    private final HeatingScheduleRepository repository;
    private final RelayConfig relayConfig;
    private final Clock clock;

    //a write hung on the database would hold every later one: it fails at the bound and the next one goes
    static final Duration WRITE_TIMEOUT = Duration.ofSeconds(30);

    //the end of the last write; the next one waits for it
    private final AtomicReference<Mono<Void>> lastWrite = new AtomicReference<>(Mono.empty());

    //read when subscribed: a write chains it after its own row is stored
    public Mono<Void> load() {
        return Mono.defer(() -> repository.findAllByOrderByIdAsc()
            .collectList()
            .doOnNext(this::distribute)
            .then());
    }

    /**
     * A new schedule of a heater, switched on. The room is named as {@code GET /rooms} names it, in any case,
     * the heater by its type ({@code radiator}, {@code floor}).
     */
    public Mono<StoredSchedule> create(final String roomName, final String heaterName, final ScheduleBody body) {
        return oneAtATime(Mono.defer(() -> {
            final Room room = room(roomName);
            final HeaterType type = drivenHeater(room, heaterName);
            final String storedRoom = room.getName().getValue();
            final String storedHeater = type.toString();

            return repository.findAllByRoomAndHeater(storedRoom, storedHeater)
                .collectList()
                .map(others -> ScheduleRules.check(body, others))
                .flatMap(valid -> {
                    final LocalDateTime now = LocalDateTime.now(clock);
                    return repository.save(new HeatingScheduleEntity(null, storedRoom, storedHeater, valid.dayNames(),
                        valid.startTime(), valid.endTime(), valid.temperature(), true, now, now));
                })
                .doOnNext(row -> log.info("Heating schedule: {} created for heater: {} in room: {}", row.id(), storedHeater, storedRoom))
                .flatMap(this::loadedAs);
        }));
    }

    /**
     * Replaces the days, the window and the temperature of a schedule; whether it is switched on stays.
     */
    public Mono<StoredSchedule> replace(final long id, final ScheduleBody body) {
        return oneAtATime(stored(id)
            .flatMap(current -> repository.findAllByRoomAndHeater(current.room(), current.heater())
                .filter(other -> !other.id().equals(id))
                .collectList()
                .map(others -> ScheduleRules.check(body, others))
                .flatMap(valid -> repository.save(new HeatingScheduleEntity(id, current.room(), current.heater(),
                    valid.dayNames(), valid.startTime(), valid.endTime(), valid.temperature(), current.enabled(),
                    current.createdAt(), LocalDateTime.now(clock)))))
            .doOnNext(row -> log.info("Heating schedule: {} replaced", id))
            .flatMap(this::loadedAs));
    }

    //switching one on needs no overlap check: a disabled schedule takes part in it while written
    public Mono<Void> enable(final long id, final boolean enabled) {
        return oneAtATime(stored(id)
            .flatMap(current -> repository.save(new HeatingScheduleEntity(id, current.room(), current.heater(),
                current.days(), current.startTime(), current.endTime(), current.temperature(), enabled,
                current.createdAt(), LocalDateTime.now(clock))))
            .doOnNext(row -> log.info("Heating schedule: {} {}", id, enabled ? "enabled" : "disabled"))
            .then(load()));
    }

    public Mono<Void> remove(final long id) {
        return oneAtATime(stored(id)
            .flatMap(repository::delete)
            .doOnSuccess(done -> log.info("Heating schedule: {} removed", id))
            .then(load()));
    }

    private Mono<HeatingScheduleEntity> stored(final long id) {
        return Mono.defer(() -> repository.findById(id)).switchIfEmpty(Mono.error(() -> ScheduleException.notFound(id)));
    }

    private Mono<StoredSchedule> loadedAs(final HeatingScheduleEntity row) {
        return load().then(Mono.fromSupplier(() -> toStored(row)));
    }

    //each write subscribes after the one before has ended, however it ended. The write is read only once it
    //subscribes, so nothing of it runs before the one ahead of it is done. It lets the next one go when it has
    //ended and the one ahead of it too: cancelled while it waited, it must not let the next one past a write
    //that is still running
    private <T> Mono<T> oneAtATime(final Mono<T> write) {
        return Mono.defer(() -> {
            final Sinks.Empty<Void> done = Sinks.empty();
            final Mono<Void> before = lastWrite.getAndSet(done.asMono());
            return before.then(write.timeout(WRITE_TIMEOUT))
                .doFinally(signal -> before.subscribe(ignored -> { }, error -> done.tryEmitEmpty(), done::tryEmitEmpty));
        });
    }

    private Room room(final String name) {
        return home.rooms().stream()
            .filter(room -> room.getName().getValue().equalsIgnoreCase(name))
            .findFirst()
            .orElseThrow(() -> new RoomNotFoundException(name));
    }

    //a heater the room has and this service drives: SANCTUM's radiator is switched outside it
    private HeaterType drivenHeater(final Room room, final String name) {
        return Arrays.stream(HeaterType.values())
            .filter(type -> type.toString().equalsIgnoreCase(name))
            .filter(type -> room.getHeaterActors().stream().anyMatch(heater -> heater.getType() == type))
            .filter(type -> relayConfig.drives(room.getName(), type))
            .findFirst()
            .orElseThrow(() -> ScheduleException.heaterNotFound(room.getName().getValue(), name));
    }

    private static StoredSchedule toStored(final HeatingScheduleEntity row) {
        return new StoredSchedule(
            row.id(),
            RoomName.fromValue(row.room()),
            Arrays.stream(HeaterType.values()).filter(type -> type.toString().equals(row.heater())).findFirst().orElseThrow(),
            row.days().stream().map(DayOfWeek::valueOf).sorted().toList(),
            row.startTime(),
            row.endTime(),
            row.temperature().doubleValue(),
            row.enabled()
        );
    }

    private void distribute(final List<HeatingScheduleEntity> rows) {
        //by identity: a heater is the object of the house, not its value
        final Map<HeaterActor, List<Schedule>> byHeater = new IdentityHashMap<>();
        //the heaters a row can name, by the names it is stored under; one the service does not drive is not
        //among them (SANCTUM's radiator is switched outside it) - a schedule would make the loop drive it
        final Map<String, HeaterActor> driven = new HashMap<>();
        home.rooms().forEach(room -> room.getHeaterActors().forEach(heater -> {
            byHeater.put(heater, new ArrayList<>());
            if (relayConfig.drives(room.getName(), heater.getType())) {
                driven.putIfAbsent(key(room.getName().getValue(), heater.getType().toString()), heater);
            }
        }));

        rows.forEach(row -> Optional.ofNullable(driven.get(key(row.room(), row.heater())))
            .ifPresentOrElse(
                heater -> toSchedule(row).ifPresent(byHeater.get(heater)::add),
                () -> log.warn(
                    "Heating schedule: {} left out - the service drives no heater: {} in room: {}",
                    row.id(), row.heater(), row.room()
                )));

        byHeater.forEach(HeaterActor::replaceSchedules);
        log.info("Heating schedules loaded: {} of {} stored", byHeater.values().stream().mapToInt(List::size).sum(), rows.size());
    }

    private static String key(final String room, final String heater) {
        return room + "|" + heater;
    }

    private Optional<Schedule> toSchedule(final HeatingScheduleEntity row) {
        try {
            return Optional.of(Schedule.builder()
                .id(row.id())
                .days(row.days().stream().map(DayOfWeek::valueOf).toList())
                .startTime(row.startTime())
                .endTime(row.endTime())
                .temperature(row.temperature().doubleValue())
                .enabled(row.enabled())
                .build());
        } catch (IllegalArgumentException exception) {
            //a day this version does not know
            log.warn("Heating schedule: {} left out - it cannot be read: {}", row.id(), exception.getMessage());
            return Optional.empty();
        }
    }
}
