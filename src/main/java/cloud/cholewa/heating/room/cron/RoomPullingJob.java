package cloud.cholewa.heating.room.cron;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomPullingJob {

    //    @Scheduled(fixedRateString = "${jobs.rooms.poolingInterval}", initialDelayString = "PT15s")
    void updateRoomsHeatersStatus() {
        log.info("Updating rooms heaters status ...");
    }

}
