package cloud.cholewa.heating.service;

import cloud.cholewa.heating.calendar.HouseCalendar;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.Schedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final Clock clock;
    private final HouseCalendar houseCalendar;

    public Mono<Room> processSchedule(final Room room) {
        return Flux.fromIterable(room.getHeaterActors())
            .doOnNext(heaterActor -> processHeaterActor(heaterActor, room))
            .then(Mono.just(room));
    }

    //logged when the decision changes: every room is decided every minute since HAS-352, and a line per
    //heater a minute would bury the switching of the relays
    private void processHeaterActor(final HeaterActor heaterActor, final Room room) {
        final boolean wasInSchedule = heaterActor.isInSchedule();
        final Double wasTarget = heaterActor.getTargetTemperature();
        heaterActor.getSchedules().stream()
            .filter(schedule -> isActiveSchedule(schedule, room.getTemperature().getValue()))
            .findFirst()
            .ifPresentOrElse(schedule -> {
                //the target first: "in schedule" is never there without one for a reader (RoomMapper)
                heaterActor.setTargetTemperature(schedule.getTemperature());
                heaterActor.setInSchedule(true);
            }, () -> {
                heaterActor.setInSchedule(false);
                heaterActor.setTargetTemperature(null);
            });

        if (wasInSchedule == heaterActor.isInSchedule() && Objects.equals(wasTarget, heaterActor.getTargetTemperature())) {
            return;
        }
        log.info(
            "Schedule for room: {}, heater actor: {} status is: {}, target temperature: {}",
            room.getName().getValue(),
            heaterActor.getType(),
            heaterActor.isInSchedule(),
            heaterActor.getTargetTemperature() == null ? "not set" : heaterActor.getTargetTemperature()
        );
    }

    private boolean isActiveSchedule(final Schedule schedule, final double temperature) {
        final LocalDateTime now = LocalDateTime.now(clock);
        return schedule.covers(now.toLocalTime(), houseCalendar.scheduleDay(now.toLocalDate()))
            && temperature < schedule.getTemperature();
    }
}
