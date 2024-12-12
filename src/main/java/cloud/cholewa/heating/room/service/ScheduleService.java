package cloud.cholewa.heating.room.service;

import cloud.cholewa.heating.infrastructure.error.HeatingException;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.Schedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    public boolean hasActiveSchedule(final Room room) {
        return getActiveSchedules(room).stream()
            .anyMatch(schedule -> room.getTemperature().getValue() < schedule.getTemperature()
                && room.getTemperature().getUpdatedAt() != null
            );
    }

    private List<Schedule> getActiveSchedules(final Room room) {
        if (room.getSchedules() == null) {
            return List.of();
        } else {
            List<Schedule> schedules = room.getSchedules().stream().filter(this::isScheduleActive).toList();
            if (schedules.size() > 1) {
                throw new HeatingException("To many schedules: [" + room.getSchedules().size()
                    + "] for room: [" + room.getName().name() + "], only one schedule is allowed");
            } else if (schedules.size() == 1) {
                log.info("Valid schedule found for room [{}], returning: {} ", room.getName().name(), schedules);
                return schedules;
            } else {
                log.info("No schedules found for room [{}]", room.getName().name());
                return List.of();
            }
        }
    }

    private boolean isScheduleActive(final Schedule schedule) {
        return LocalTime.now().isAfter(schedule.getStartTime())
            && LocalTime.now().isBefore(schedule.getEndTime())
            && isDayOfWeekValid(schedule);
    }

    private boolean isDayOfWeekValid(final Schedule schedule) {
        return schedule.getDays().contains(LocalDate.now().getDayOfWeek());
    }
}
