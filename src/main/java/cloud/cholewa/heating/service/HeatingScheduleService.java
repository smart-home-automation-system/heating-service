package cloud.cholewa.heating.service;

import cloud.cholewa.heating.config.RelayConfig;
import cloud.cholewa.heating.db.model.HeatingScheduleEntity;
import cloud.cholewa.heating.db.repository.HeatingScheduleRepository;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Schedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The heating schedules of the heaters, stored in {@code heating_schedule} (HAS-355). They are read whole
 * into the heaters of {@link Home} - at the start, and after every write once there is one - and the control
 * loop keeps reading them from memory, never from the database at a reading.
 * <p>
 * A row this version cannot place is logged and left out, the others are loaded: a room or a heater the
 * house does not have or this service does not drive (SANCTUM), a name or a day it does not know. A heater without a row has no schedule and is
 * never in one.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeatingScheduleService {

    private final Home home;
    private final HeatingScheduleRepository repository;
    private final RelayConfig relayConfig;

    public Mono<Void> load() {
        return repository.findAllByOrderByIdAsc()
            .collectList()
            .doOnNext(this::distribute)
            .then();
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
