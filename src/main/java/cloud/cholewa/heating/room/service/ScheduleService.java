package cloud.cholewa.heating.room.service;

import cloud.cholewa.heating.infrastructure.error.HeatingException;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.Schedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    public Schedule findSchedule(final Room room) {
        if (room.getSchedules() == null) {
            return null;
        }

        List<Schedule> schedules = getActiveSchedules(room);

        if (schedules.size() > 1) {
            throw new HeatingException("To many schedules [{" + schedules.size() + "}], only one schedule is allowed");
        }

        return schedules.stream().findFirst().orElse(null);
    }

    private List<Schedule> getActiveSchedules(final Room room) {
        return room.getSchedules() == null
            ? List.of()
            : room.getSchedules().stream().filter(this::isScheduleActive).toList();
    }

    private boolean isScheduleActive(final Schedule schedule) {
        return LocalTime.now().isAfter(schedule.getStartTime()) && LocalTime.now().isBefore(schedule.getEndTime());
    }
}
