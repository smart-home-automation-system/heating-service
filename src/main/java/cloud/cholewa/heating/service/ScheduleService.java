package cloud.cholewa.heating.service;

import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.Schedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final Clock clock;

    public Mono<Room> processSchedule(final Room room) {
        return Flux.fromIterable(room.getHeaterActors())
            .doOnNext(heaterActor -> processHeaterActor(heaterActor, room))
            .then(Mono.just(room));
    }

    private void processHeaterActor(final HeaterActor heaterActor, final Room room) {
        heaterActor.getSchedules().stream()
            .filter(schedule -> isActiveSchedule(schedule, room.getTemperature().getValue()))
            .findFirst()
            .ifPresentOrElse(schedule -> {
                heaterActor.setInSchedule(true);
                heaterActor.setTargetTemperature(schedule.getTemperature());
            }, () -> {
                heaterActor.setInSchedule(false);
                heaterActor.setTargetTemperature(null);
            });

        log.info(
            "Schedule for room: {}, heater actor: {} status is: {}, target temperature: {}",
            room.getName().getValue(),
            heaterActor.getType(),
            heaterActor.isInSchedule(),
            heaterActor.getTargetTemperature()
        );
    }

    private boolean isActiveSchedule(final Schedule schedule, final double temperature) {
        return schedule.getStartTime().isBefore(LocalTime.now(clock))
            && schedule.getEndTime().isAfter(LocalTime.now(clock))
            && schedule.getDays().stream().anyMatch(
            dayOfWeek -> dayOfWeek.equals(LocalDate.now(clock).getDayOfWeek()))
            && temperature < schedule.getTemperature();
    }
}
