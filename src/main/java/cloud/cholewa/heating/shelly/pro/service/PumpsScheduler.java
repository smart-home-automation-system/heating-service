package cloud.cholewa.heating.shelly.pro.service;

import cloud.cholewa.heating.config.job.PumpsPoolingJobConfig;
import cloud.cholewa.heating.model.BoilerRoom;
import cloud.cholewa.heating.model.HeatingSource;
import cloud.cholewa.heating.model.HeatingSourceType;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Pump;
import cloud.cholewa.heating.model.PumpType;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.shelly.pro.client.ShellyProClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PumpsScheduler {

    private final PumpsPoolingJobConfig pumpsPoolingJobConfig;
    private final Home home;
    private final ShellyProClient shellyProClient;

    @Scheduled(fixedRateString = "${jobs.pumps.poolingInterval}")
    void handlePumps() {
        BoilerRoom boilerRoom = home.getBoiler();
        Pump hotWaterPump = boilerRoom.getPumps().stream()
            .filter(pump -> pump.getType().equals(PumpType.HOT_WATER_PUMP))
            .findFirst().orElseThrow();
        Pump fireplacePump = boilerRoom.getPumps().stream()
            .filter(pump -> pump.getType().equals(PumpType.FIREPLACE_PUMP))
            .findFirst().orElseThrow();
        Pump heatingPump = boilerRoom.getPumps().stream()
            .filter(pump -> pump.getType().equals(PumpType.HEATING_PUMP))
            .findFirst().orElseThrow();

        HeatingSource furnace = boilerRoom.getHeatingSources().stream()
            .filter(heatingSource -> heatingSource.getType().equals(HeatingSourceType.FURNACE))
            .findFirst().orElseThrow();

        //hot water has the highest priority
//        if (hotWaterPump.isRunning()) {
//            shellyProClient.controlHotWaterPump().subscribe();
//        }

//        shellyProClient.controlHotWaterPump(hotWaterPump.isRunning()).subscribe();

        if (hotWaterPump.isRunning() || heatingPump.isRunning()) {
            if (!furnace.isActive()) {
                furnace.setActive(true);
                shellyProClient.controlFurnace(true).subscribe();
            }
        }

        if (!hotWaterPump.isRunning() && fireplacePump.isRunning()) {
            if (furnace.isActive()) {
                furnace.setActive(false);
                shellyProClient.controlFurnace(false).subscribe();
            }
        }

        if (home.isHeatingAllowed()) {
            if (home.getRooms().stream().anyMatch(Room::isHeatingActive)) {
                if (!furnace.isActive()) {
                    shellyProClient.controlHeatingPump(true).subscribe();
                    heatingPump.setRunning(true);
                    shellyProClient.controlFurnace(true).subscribe();
                    furnace.setActive(true);
                    furnace.setUpdateTime(LocalDateTime.now());
                }
            } else {
                if (furnace.isActive()) {
                    shellyProClient.controlHeatingPump(false).subscribe();
                    heatingPump.setRunning(false);
                    shellyProClient.controlFurnace(false).subscribe();
                    furnace.setActive(false);
                    furnace.setUpdateTime(LocalDateTime.now());
                }
            }
        }

        if (!home.isHeatingAllowed()) {
            if (furnace.isActive()) {
                shellyProClient.controlHeatingPump(false).subscribe();
                heatingPump.setRunning(false);
                shellyProClient.controlFurnace(false).subscribe();
                furnace.setActive(false);
                furnace.setUpdateTime(LocalDateTime.now());
            }
        }
    }
}
