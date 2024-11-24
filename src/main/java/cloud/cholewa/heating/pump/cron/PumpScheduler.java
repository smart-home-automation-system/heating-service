package cloud.cholewa.heating.pump.cron;

import cloud.cholewa.heating.model.Furnace;
import cloud.cholewa.heating.model.HotWater;
import cloud.cholewa.heating.model.Pump;
import cloud.cholewa.heating.shelly.actor.BoilerPro4Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class PumpScheduler {

    private static final double HOT_WATER_LOW_TEMPERATURE = 38;
    private static final double HOT_WATER_HIGH_TEMPERATURE = 42;

    private final HotWater hotWater;
    private final Pump fireplacePump;
    private final Pump floorPump;
    private final Pump heatingPump;
    private final Pump hotWaterPump;

    private final BoilerPro4Client boilerPro4Client;
    private final Furnace furnace;

//    private final Home home;
//    private final ShellyProClient shellyProClient;

    @Scheduled(fixedRateString = "${jobs.pumps.poolingInterval}", initialDelayString = "PT5s")
    void handleBoiler() {
        Pump circulationPump = hotWater.circulation().pump();

        queryPumpStatus();

//        BoilerRoom boilerRoom = home.getBoiler();
//        Pump hotWaterPump = boilerRoom.getPumps().stream()
//            .filter(pump -> pump.getType().equals(PumpType.HOT_WATER_PUMP))
//            .findFirst().orElseThrow();
//        Pump fireplacePump = boilerRoom.getPumps().stream()
//            .filter(pump -> pump.getType().equals(PumpType.FIREPLACE_PUMP))
//            .findFirst().orElseThrow();
//        Pump heatingPump = boilerRoom.getPumps().stream()
//            .filter(pump -> pump.getType().equals(PumpType.HEATING_PUMP))
//            .findFirst().orElseThrow();
//
//        HeatingSource furnace = boilerRoom.getHeatingSources().stream()
//            .filter(heatingSource -> heatingSource.getType().equals(HeatingSourceType.FURNACE))
//            .findFirst().orElseThrow();

        //hot water has the highest priority
//        if (hotWaterPump.isRunning()) {
//            shellyProClient.controlHotWaterPump().subscribe();
//        }

//        shellyProClient.controlHotWaterPump(hotWaterPump.isRunning()).subscribe();

//        if (hotWaterPump.isRunning() || heatingPump.isRunning()) {
//            if (!furnace.isActive()) {
//                furnace.setActive(true);
//                shellyProClient.controlFurnace(true).subscribe();
//            }
//        }
//
//        if (!hotWaterPump.isRunning() && fireplacePump.isRunning()) {
//            if (furnace.isActive()) {
//                furnace.setActive(false);
//                shellyProClient.controlFurnace(false).subscribe();
//            }
//        }
//
//        if (home.isHeatingAllowed()) {
//            if (home.getRooms().stream().anyMatch(Room::isHeatingActive)) {
//                if (!furnace.isActive()) {
//                    shellyProClient.controlHeatingPump(true).subscribe();
//                    heatingPump.setRunning(true);
//                    shellyProClient.controlFurnace(true).subscribe();
//                    furnace.setActive(true);
//                    furnace.setUpdateTime(LocalDateTime.now());
//                }
//            } else {
//                if (furnace.isActive()) {
//                    shellyProClient.controlHeatingPump(false).subscribe();
//                    heatingPump.setRunning(false);
//                    shellyProClient.controlFurnace(false).subscribe();
//                    furnace.setActive(false);
//                    furnace.setUpdateTime(LocalDateTime.now());
//                }
//            }
//        }
//
//        if (!home.isHeatingAllowed()) {
//            if (furnace.isActive() && !hotWaterPump.isRunning()) {
//                shellyProClient.controlHeatingPump(false).subscribe();
//                heatingPump.setRunning(false);
//                shellyProClient.controlFurnace(false).subscribe();
//                furnace.setActive(false);
//                furnace.setUpdateTime(LocalDateTime.now());
//            }
//        }
    }

    private void queryPumpStatus() {
        log.info("Querying pump status for Boiler");
        Flux.interval(Duration.ofSeconds(3))
            .take(5)
            .flatMap(i ->
                switch (i.intValue()) {
                    case 1 -> queryHotWaterPumpStatus();
                    case 2 -> queryHeatingPumpStatus();
                    case 3 -> queryFireplacePumpStatus();
                    case 4 -> queryFloorPumpStatus();
                    case 5 -> queryFurnaceStatus();
                    default -> Mono.empty();
                })
            .subscribe();
    }

    private Mono<Void> queryHotWaterPumpStatus() {
        return boilerPro4Client.getHotWaterPumpStatus()
            .doOnError(throwable -> log.error("Error while querying hot water pump status", throwable))
            .flatMap(response -> {
                    hotWaterPump.setRunning(Boolean.TRUE.equals(response.getOutput()));
                    return Mono.empty();
                }
            );
    }

    private Mono<Void> queryHeatingPumpStatus() {
        return boilerPro4Client.getHeatingPumpStatus()
            .doOnError(throwable -> log.error("Error while querying hot water pump status", throwable))
            .flatMap(response -> {
                    heatingPump.setRunning(Boolean.TRUE.equals(response.getOutput()));
                    return Mono.empty();
                }
            );
    }

    private Mono<Void> queryFireplacePumpStatus() {
        return boilerPro4Client.getFireplacePumpStatus()
            .doOnError(throwable -> log.error("Error while querying fireplace pump status", throwable))
            .flatMap(response -> {
                    fireplacePump.setRunning(Boolean.TRUE.equals(response.getOutput()));
                    return Mono.empty();
                }
            );
    }

    private Mono<Void> queryFloorPumpStatus() {
        return Mono.empty();
    }

    private Mono<Void> queryFurnaceStatus() {
        return boilerPro4Client.getFurnaceStatus()
            .doOnError(throwable -> log.error("Error while querying furnace status", throwable))
            .flatMap(response -> {
                    furnace.setRunning(Boolean.TRUE.equals(response.getOutput()));
                    return Mono.empty();
                }
            );
    }
}
