package cloud.cholewa.heating.room.cron;

import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.room.service.BathroomDownRoomService;
import cloud.cholewa.heating.room.service.BathroomUpRoomService;
import cloud.cholewa.heating.room.service.BedroomRoomService;
import cloud.cholewa.heating.room.service.CinemaRoomService;
import cloud.cholewa.heating.room.service.EntranceRoomService;
import cloud.cholewa.heating.room.service.GarageRoomService;
import cloud.cholewa.heating.room.service.LiviaRoomService;
import cloud.cholewa.heating.room.service.LivingRoomRoomService;
import cloud.cholewa.heating.room.service.OfficeRoomService;
import cloud.cholewa.heating.room.service.TobiRoomService;
import cloud.cholewa.heating.room.service.WardrobeRoomService;
import cloud.cholewa.heating.shelly.actor.HeaterPro4Client;
import cloud.cholewa.heating.shelly.actor.HeaterPro4Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static cloud.cholewa.home.model.RoomName.BATHROOM_DOWN;
import static cloud.cholewa.home.model.RoomName.BATHROOM_UP;
import static cloud.cholewa.home.model.RoomName.BEDROOM;
import static cloud.cholewa.home.model.RoomName.CINEMA;
import static cloud.cholewa.home.model.RoomName.ENTRANCE;
import static cloud.cholewa.home.model.RoomName.GARAGE;
import static cloud.cholewa.home.model.RoomName.KITCHEN;
import static cloud.cholewa.home.model.RoomName.LIVIA;
import static cloud.cholewa.home.model.RoomName.LIVING_ROOM;
import static cloud.cholewa.home.model.RoomName.OFFICE;
import static cloud.cholewa.home.model.RoomName.TOBI;
import static cloud.cholewa.home.model.RoomName.WARDROBE;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomPullingJob {

    private final Room office;
    private final Room tobi;
    private final Room livia;
    private final Room bedroom;
    private final Room wardrobe;
    private final Room bathroomUp;
    private final Room livingRoom;
    private final Room cinema;
    private final Room bathroomDown;
    private final Room entrance;
    private final Room garage;

    private final OfficeRoomService officeRoomService;
    private final TobiRoomService tobiRoomService;
    private final LiviaRoomService liviaRoomService;
    private final BedroomRoomService bedroomRoomService;
    private final WardrobeRoomService wardrobeRoomService;
    private final BathroomUpRoomService bathroomUpRoomService;
    private final LivingRoomRoomService livingRoomRoomService;
    private final BathroomDownRoomService bathroomDownRoomService;
    private final CinemaRoomService cinemaRoomService;
    private final EntranceRoomService entranceRoomService;
    private final GarageRoomService garageRoomService;

    private final HeaterPro4Config heaterPro4Config;
    private final HeaterPro4Client heaterPro4Client;

//    @Scheduled(fixedRateString = "${jobs.rooms.poolingInterval}", initialDelayString = "PT15s")
    void updateRoomsHeatersStatus() {
        log.info("Updating rooms heaters status ...");

        queryRoomsStatus();
    }

    private void queryRoomsStatus() {
        Flux.interval(Duration.ofSeconds(2))
            .take(11)
            .flatMap(i ->
                switch (i.intValue() + 1) {
                    case 1 -> queryOffice();
                    case 2 -> queryTobi();
                    case 3 -> queryLivia();
                    case 4 -> queryBedroom();
                    case 5 -> queryWardrobe();
                    case 6 -> queryBathroomUp();
                    case 7 -> queryLivingRoom();
                    case 8 -> queryBathroomDown();
                    case 9 -> queryCinema();
                    case 10 -> queryEntrance();
                    case 11 -> queryGarage();
                    default -> Mono.empty();
                }
            )
            .subscribe();
    }

    private Mono<Void> queryOffice() {
        HeaterActor officeHeater = office.getHeaterActors().stream()
            .filter(heaterActor -> heaterActor.getName().equals(HeaterType.RADIATOR))
            .findFirst().orElseThrow();

        return heaterPro4Client.getHeaterActorStatus(heaterPro4Config.getShellyPro4HeaterHost(OFFICE))
            .doOnError(throwable -> log.error("Error while querying hot [OFFICE HEATER] status", throwable))
            .doOnNext(response ->
                log.info(
                    "Received heater status [OFFICE HEATER] isWorking: {}",
                    response.getOutput()
                ))
            .flatMap(response -> {
                    officeHeater.setWorking(Boolean.TRUE.equals(response.getOutput()));
                    return Mono.just(response);
                }
            )
            .flatMap(response -> officeRoomService.handleOfficeRoom());
    }

    private Mono<Void> queryTobi() {
        HeaterActor tobiHeater = tobi.getHeaterActors().stream()
            .filter(heaterActor -> heaterActor.getName().equals(HeaterType.RADIATOR))
            .findFirst().orElseThrow();

        return heaterPro4Client.getHeaterActorStatus(heaterPro4Config.getShellyPro4HeaterHost(TOBI))
            .doOnError(throwable -> log.error("Error while querying hot [TOBI HEATER] status", throwable))
            .doOnNext(response ->
                log.info(
                    "Received heater status [TOBI HEATER] isWorking: {}",
                    response.getOutput()
                ))
            .flatMap(response -> {
                    tobiHeater.setWorking(Boolean.TRUE.equals(response.getOutput()));
                    return Mono.just(response);
                }
            )
            .flatMap(response -> tobiRoomService.handleTobiRoom());
    }

    private Mono<Void> queryLivia() {
        HeaterActor liviaHeater = livia.getHeaterActors().stream()
            .filter(heaterActor -> heaterActor.getName().equals(HeaterType.RADIATOR))
            .findFirst().orElseThrow();

        return heaterPro4Client.getHeaterActorStatus(heaterPro4Config.getShellyPro4HeaterHost(LIVIA))
            .doOnError(throwable -> log.error("Error while querying hot [LIVIA HEATER] status", throwable))
            .doOnNext(response ->
                log.info(
                    "Received heater status [LIVIA HEATER] isWorking: {}",
                    response.getOutput()
                ))
            .flatMap(response -> {
                    liviaHeater.setWorking(Boolean.TRUE.equals(response.getOutput()));
                    return Mono.just(response);
                }
            )
            .flatMap(response -> liviaRoomService.handleLiviaRoom());
    }

    private Mono<Void> queryBedroom() {
        HeaterActor bedroomHeater = bedroom.getHeaterActors().stream()
            .filter(heaterActor -> heaterActor.getName().equals(HeaterType.RADIATOR))
            .findFirst().orElseThrow();

        return heaterPro4Client.getHeaterActorStatus(heaterPro4Config.getShellyPro4HeaterHost(BEDROOM))
            .doOnError(throwable -> log.error("Error while querying hot [BEDROOM HEATER] status", throwable))
            .doOnNext(response ->
                log.info(
                    "Received heater status [BEDROOM HEATER] isWorking: {}",
                    response.getOutput()
                ))
            .flatMap(response -> {
                    bedroomHeater.setWorking(Boolean.TRUE.equals(response.getOutput()));
                    return Mono.just(response);
                }
            )
            .flatMap(response -> bedroomRoomService.handleBedroomRoom());
    }

    private Mono<Void> queryWardrobe() {
        HeaterActor floorWardrobe = wardrobe.getHeaterActors().stream()
            .filter(heaterActor -> heaterActor.getName().equals(HeaterType.FLOOR))
            .findFirst().orElseThrow();

        return heaterPro4Client.getHeaterActorStatus(heaterPro4Config.getShellyPro4FloorHost(WARDROBE))
            .doOnError(throwable -> log.error("Error while querying hot [WARDROBE FLOOR] status", throwable))
            .doOnNext(response ->
                log.info(
                    "Received heater status [WARDROBE FLOOR] isWorking: {}",
                    response.getOutput()
                ))
            .flatMap(response -> {
                    floorWardrobe.setWorking(Boolean.TRUE.equals(response.getOutput()));
                    return Mono.just(response);
                }
            )
            .flatMap(response -> wardrobeRoomService.handleWardrobeRoom());
    }

    private Mono<Void> queryBathroomUp() {
        HeaterActor floorBathroomUp = bathroomUp.getHeaterActors().stream()
            .filter(heaterActor -> heaterActor.getName().equals(HeaterType.FLOOR))
            .findFirst().orElseThrow();

        HeaterActor heaterBathroomUp = bathroomUp.getHeaterActors().stream()
            .filter(heaterActor -> heaterActor.getName().equals(HeaterType.RADIATOR))
            .findFirst().orElseThrow();

        return heaterPro4Client.getHeaterActorStatus(heaterPro4Config.getShellyPro4FloorHost(BATHROOM_UP))
            .doOnError(throwable -> log.error("Error while querying hot [BATHROOM UP FLOOR] status", throwable))
            .doOnNext(response ->
                log.info(
                    "Received heater status [BATHROOM UP FLOOR] isWorking: {}",
                    response.getOutput()
                ))
            .flatMap(response -> {
                    floorBathroomUp.setWorking(Boolean.TRUE.equals(response.getOutput()));
                    return Mono.just(response);
                }
            )
            .then(Mono.defer(() -> heaterPro4Client.getHeaterActorStatus(heaterPro4Config.getShellyPro4HeaterHost(
                    BATHROOM_UP))
                .doOnError(throwable -> log.error("Error while querying hot [BATHROOM UP HEATER] status", throwable))
                .doOnNext(response ->
                    log.info(
                        "Received heater status [BATHROOM UP HEATER] isWorking: {}",
                        response.getOutput()
                    ))
                .flatMap(response -> {
                        heaterBathroomUp.setWorking(Boolean.TRUE.equals(response.getOutput()));
                        return Mono.just(response);
                    }
                )
                .flatMap(response -> bathroomUpRoomService.handleBathroomUpRoom())));
    }

    private Mono<Void> queryLivingRoom() {
        HeaterActor floorLivingRoom = livingRoom.getHeaterActors().stream()
            .filter(heaterActor -> heaterActor.getName().equals(HeaterType.FLOOR))
            .findFirst().orElseThrow();

        HeaterActor heaterLivingRoom = livingRoom.getHeaterActors().stream()
            .filter(heaterActor -> heaterActor.getName().equals(HeaterType.RADIATOR))
            .findFirst().orElseThrow();

        return heaterPro4Client.getHeaterActorStatus(heaterPro4Config.getShellyPro4FloorHost(KITCHEN))
            .doOnError(throwable -> log.error("Error while querying hot [KITCHEN FLOOR] status", throwable))
            .doOnNext(response ->
                log.info(
                    "Received heater status [KITCHEN FLOOR] isWorking: {}",
                    response.getOutput()
                ))
            .flatMap(response -> {
                    floorLivingRoom.setWorking(Boolean.TRUE.equals(response.getOutput()));
                    return Mono.just(response);
                }
            )
            .then(Mono.defer(() -> heaterPro4Client.getHeaterActorStatus(heaterPro4Config.getShellyPro4HeaterHost(
                    LIVING_ROOM))
                .doOnError(throwable -> log.error("Error while querying hot [LIVING ROOM HEATER] status", throwable))
                .doOnNext(response ->
                    log.info(
                        "Received heater status [LIVING ROOM HEATER] isWorking: {}",
                        response.getOutput()
                    ))
                .flatMap(response -> {
                        heaterLivingRoom.setWorking(Boolean.TRUE.equals(response.getOutput()));
                        return Mono.just(response);
                    }
                )
                .flatMap(response -> livingRoomRoomService.handleLivingRoom())));
    }

    private Mono<Void> queryBathroomDown() {
        HeaterActor floorBathroomDown = bathroomDown.getHeaterActors().stream()
            .filter(heaterActor -> heaterActor.getName().equals(HeaterType.FLOOR))
            .findFirst().orElseThrow();

        HeaterActor heaterBathroomDown = bathroomDown.getHeaterActors().stream()
            .filter(heaterActor -> heaterActor.getName().equals(HeaterType.RADIATOR))
            .findFirst().orElseThrow();

        return heaterPro4Client.getHeaterActorStatus(heaterPro4Config.getShellyPro4FloorHost(BATHROOM_DOWN))
            .doOnError(throwable -> log.error("Error while querying hot [BATHROOM DOWN FLOOR] status", throwable))
            .doOnNext(response ->
                log.info(
                    "Received heater status [BATHROOM DOWN FLOOR] isWorking: {}",
                    response.getOutput()
                ))
            .flatMap(response -> {
                    floorBathroomDown.setWorking(Boolean.TRUE.equals(response.getOutput()));
                    return Mono.just(response);
                }
            )
            .then(Mono.defer(() -> heaterPro4Client.getHeaterActorStatus(heaterPro4Config.getShellyPro4HeaterHost(
                    BATHROOM_UP))
                .doOnError(throwable -> log.error("Error while querying hot [BATHROOM DOWN HEATER] status", throwable))
                .doOnNext(response ->
                    log.info(
                        "Received heater status [BATHROOM DOWN HEATER] isWorking: {}",
                        response.getOutput()
                    ))
                .flatMap(response -> {
                        heaterBathroomDown.setWorking(Boolean.TRUE.equals(response.getOutput()));
                        return Mono.just(response);
                    }
                )
                .flatMap(response -> bathroomDownRoomService.handleBathroomDownRoom())));
    }

    private Mono<Void> queryCinema() {
        HeaterActor cinemaHeater = cinema.getHeaterActors().stream()
            .filter(heaterActor -> heaterActor.getName().equals(HeaterType.RADIATOR))
            .findFirst().orElseThrow();

        return heaterPro4Client.getHeaterActorStatus(heaterPro4Config.getShellyPro4HeaterHost(CINEMA))
            .doOnError(throwable -> log.error("Error while querying hot [CINEMA HEATER] status", throwable))
            .doOnNext(response ->
                log.info(
                    "Received heater status [CINEMA HEATER] isWorking: {}",
                    response.getOutput()
                ))
            .flatMap(response -> {
                    cinemaHeater.setWorking(Boolean.TRUE.equals(response.getOutput()));
                    return Mono.just(response);
                }
            )
            .flatMap(response -> cinemaRoomService.handleCinemaRoom());
    }

    private Mono<Void> queryEntrance() {
        HeaterActor entranceHeater = entrance.getHeaterActors().stream()
            .filter(heaterActor -> heaterActor.getName().equals(HeaterType.RADIATOR))
            .findFirst().orElseThrow();

        return heaterPro4Client.getHeaterActorStatus(heaterPro4Config.getShellyPro4HeaterHost(ENTRANCE))
            .doOnError(throwable -> log.error("Error while querying hot [ENTRANCE HEATER] status", throwable))
            .doOnNext(response ->
                log.info(
                    "Received heater status [ENTRANCE HEATER] isWorking: {}",
                    response.getOutput()
                ))
            .flatMap(response -> {
                    entranceHeater.setWorking(Boolean.TRUE.equals(response.getOutput()));
                    return Mono.just(response);
                }
            )
            .flatMap(response -> entranceRoomService.handleEntranceRoom());
    }

    private Mono<Void> queryGarage() {
        HeaterActor garageHeater = garage.getHeaterActors().stream()
            .filter(heaterActor -> heaterActor.getName().equals(HeaterType.RADIATOR))
            .findFirst().orElseThrow();

        return heaterPro4Client.getHeaterActorStatus(heaterPro4Config.getShellyPro4HeaterHost(GARAGE))
            .doOnError(throwable -> log.error("Error while querying hot [GARAGE HEATER] status", throwable))
            .doOnNext(response ->
                log.info(
                    "Received heater status [GARAGE HEATER] isWorking: {}",
                    response.getOutput()
                ))
            .flatMap(response -> {
                    garageHeater.setWorking(Boolean.TRUE.equals(response.getOutput()));
                    return Mono.just(response);
                }
            )
            .flatMap(response -> garageRoomService.handleGarageRoom());
    }
}
