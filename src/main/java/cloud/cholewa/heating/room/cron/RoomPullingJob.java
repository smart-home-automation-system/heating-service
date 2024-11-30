package cloud.cholewa.heating.room.cron;

import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.room.service.OfficeRoomService;
import cloud.cholewa.heating.shelly.actor.HeaterPro4Client;
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
public class RoomPullingJob {

    private final Room office;

    private final OfficeRoomService officeRoomService;

    private final HeaterPro4Client heaterPro4Client;

    @Scheduled(fixedRateString = "${jobs.rooms.poolingInterval}", initialDelayString = "PT15s")
    void updateRoomsHeatersStatus() {
        log.info("Updating rooms heaters status ...");

        queryRoomsStatus();

        officeRoomService.handleOfficeRoom();
    }

    private void queryRoomsStatus() {
        Flux.interval(Duration.ofSeconds(2))
            .take(1)
            .flatMap(i ->
                switch (i.intValue() + 1) {
                    case 1 -> queryOffice();
                    default -> Mono.empty();
                }
            )
            .subscribe();
    }

    private Mono<Void> queryOffice() {
        HeaterActor officeHeater = office.getHeaterActors().stream()
            .filter(heaterActor -> heaterActor.getName().equals(HeaterType.RADIATOR))
            .findFirst().orElseThrow();

        return heaterPro4Client.getOfficeStatus()
            .doOnError(throwable -> log.error("Error while querying hot [OFFICE HEATER] status", throwable))
            .doOnNext(response ->
                log.info(
                    "Received heater status [OFFICE HEATER] isWorking: {}",
                    response.getOutput()
                ))
            .flatMap(response -> {
                    officeHeater.setWorking(Boolean.TRUE.equals(response.getOutput()));
                    return Mono.empty();
                }
            );
    }
}
