package cloud.cholewa.heating.air.service;

import cloud.cholewa.heating.air.client.AirShellyProClient;
import cloud.cholewa.heating.model.HeatingSourceType;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Room;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AirHeatingService {

    private final Home home;
    private final AirShellyProClient airShellyProClient;

    public void controlHeaterSource(final Room room) {

        switch (room.getName()) {
            case LIVING_ROOM -> updateLivingRoom(room);
            case BATHROOM_DOWN -> updateBathRoomDown(room);
            case CINEMA -> updateCinema(room);
            case BATHROOM_UP -> updateBathRoomUp(room);
            case BEDROOM -> updateBedroom(room);
            case LIVIA -> updateLivia(room);
            case TOBI -> updateTobi(room);
            case OFFICE -> updateOffice(room);
        }
    }

    private void updateOffice(final Room room) {
        if (LocalTime.now().isAfter(LocalTime.of(5, 0)) && LocalTime.now().isBefore(LocalTime.of(23, 0))) {
            if (home.isHeatingAllowed() || isFireplaceWorking()) {
                if (room.getTemperatureSensor().getTemperature() < 20.5 && !room.isHeatingActive()) {
                    room.setHeatingActive(true);
                    airShellyProClient.controlOffice(true).subscribe();
                    log.info(
                        "Office heating is active, current temperature is [{}C]",
                        room.getTemperatureSensor().getTemperature()
                    );
                } else if (room.getTemperatureSensor().getTemperature() > 20.5 && room.isHeatingActive()) {
                    room.setHeatingActive(false);
                    airShellyProClient.controlOffice(false).subscribe();
                    log.info(
                        "Office heating is disabled, current temperature is [{}C]",
                        room.getTemperatureSensor().getTemperature()
                    );
                }
            }
        } else {
            if (room.isHeatingActive()) {
                room.setHeatingActive(false);
                airShellyProClient.controlOffice(false).subscribe();
            }
        }
    }

    private void updateTobi(final Room room) {
        if (LocalTime.now().isAfter(LocalTime.of(5, 0)) && LocalTime.now().isBefore(LocalTime.of(23, 0))) {
            if (home.isHeatingAllowed() || isFireplaceWorking()) {
                if (room.getTemperatureSensor().getTemperature() < 20.5 && !room.isHeatingActive()) {
                    room.setHeatingActive(true);
                    airShellyProClient.controlTobi(true).subscribe();
                    log.info(
                        "Tobi heating is active, current temperature is [{}C]",
                        room.getTemperatureSensor().getTemperature()
                    );
                } else if (room.getTemperatureSensor().getTemperature() > 20.5 && room.isHeatingActive()) {
                    room.setHeatingActive(false);
                    airShellyProClient.controlTobi(false).subscribe();
                    log.info(
                        "Tobi heating is disabled, current temperature is [{}C]",
                        room.getTemperatureSensor().getTemperature()
                    );
                }
            }
        } else {
            if (room.isHeatingActive()) {
                room.setHeatingActive(false);
                airShellyProClient.controlTobi(false).subscribe();
            }
        }
    }

    private void updateLivia(final Room room) {
        if (LocalTime.now().isAfter(LocalTime.of(5, 0)) && LocalTime.now().isBefore(LocalTime.of(23, 0))) {
            if (home.isHeatingAllowed() || isFireplaceWorking()) {
                if (room.getTemperatureSensor().getTemperature() < 20.5 && !room.isHeatingActive()) {
                    room.setHeatingActive(true);
                    airShellyProClient.controlLivia(true).subscribe();
                    log.info(
                        "Livia heating is active, current temperature is [{}C]",
                        room.getTemperatureSensor().getTemperature()
                    );
                } else if (room.getTemperatureSensor().getTemperature() > 20.5 && room.isHeatingActive()) {
                    room.setHeatingActive(false);
                    airShellyProClient.controlLivia(false).subscribe();
                    log.info(
                        "Livia heating is disabled, current temperature is [{}C]",
                        room.getTemperatureSensor().getTemperature()
                    );
                }
            }
        } else {
            if (room.isHeatingActive()) {
                room.setHeatingActive(false);
                airShellyProClient.controlLivia(false).subscribe();
            }
        }
    }

    private void updateBedroom(final Room room) {
        if (LocalTime.now().isAfter(LocalTime.of(5, 0)) && LocalTime.now().isBefore(LocalTime.of(23, 0))) {
            if (home.isHeatingAllowed() || isFireplaceWorking()) {
                if (room.getTemperatureSensor().getTemperature() < 20.5 && !room.isHeatingActive()) {
                    room.setHeatingActive(true);
                    airShellyProClient.controlBedroom(true).subscribe();
                    log.info(
                        "Bedroom heating is active, current temperature is [{}C]",
                        room.getTemperatureSensor().getTemperature()
                    );
                } else if (room.getTemperatureSensor().getTemperature() > 20.5 && room.isHeatingActive()) {
                    room.setHeatingActive(false);
                    airShellyProClient.controlBedroom(false).subscribe();
                    log.info(
                        "Bedroom heating is disabled, current temperature is [{}C]",
                        room.getTemperatureSensor().getTemperature()
                    );
                }
            }
        } else {
            if (room.isHeatingActive()) {
                room.setHeatingActive(false);
                airShellyProClient.controlBedroom(false).subscribe();
            }
        }
    }

    private void updateBathRoomUp(final Room room) {
        if (LocalTime.now().isAfter(LocalTime.of(5, 0)) && LocalTime.now().isBefore(LocalTime.of(23, 0))) {
            if (home.isHeatingAllowed() || isFireplaceWorking()) {
                if (room.getTemperatureSensor().getTemperature() < 20.5 && !room.isHeatingActive()) {
                    room.setHeatingActive(true);
                    airShellyProClient.controlBathUp(true).subscribe();
                    log.info(
                        "Bathroom up heating is active, current temperature is [{}C]",
                        room.getTemperatureSensor().getTemperature()
                    );
                } else if (room.getTemperatureSensor().getTemperature() > 20.5 && room.isHeatingActive()) {
                    room.setHeatingActive(false);
                    airShellyProClient.controlBathUp(false).subscribe();
                    log.info(
                        "Bathroom up heating is disabled, current temperature is [{}C]",
                        room.getTemperatureSensor().getTemperature()
                    );
                }
            }
        } else {
            if (room.isHeatingActive()) {
                room.setHeatingActive(false);
                airShellyProClient.controlBathUp(false).subscribe();
            }
        }
    }

    private void updateCinema(final Room room) {
        if (LocalTime.now().isAfter(LocalTime.of(5, 0)) && LocalTime.now().isBefore(LocalTime.of(23, 0))) {
            if (home.isHeatingAllowed() || isFireplaceWorking()) {
                if (room.getTemperatureSensor().getTemperature() < 20.5 && !room.isHeatingActive()) {
                    room.setHeatingActive(true);
                    airShellyProClient.controlCinema(true).subscribe();
                    log.info(
                        "Cinema heating is active, current temperature is [{}C]",
                        room.getTemperatureSensor().getTemperature()
                    );
                } else if (room.getTemperatureSensor().getTemperature() > 20.5 && room.isHeatingActive()) {
                    room.setHeatingActive(false);
                    airShellyProClient.controlCinema(false).subscribe();
                    log.info(
                        "Cinema heating is disabled, current temperature is [{}C]",
                        room.getTemperatureSensor().getTemperature()
                    );
                }
            }
        } else {
            if (room.isHeatingActive()) {
                room.setHeatingActive(false);
                airShellyProClient.controlCinema(false).subscribe();
            }
        }
    }

    private void updateBathRoomDown(final Room room) {
        if (LocalTime.now().isAfter(LocalTime.of(5, 0)) && LocalTime.now().isBefore(LocalTime.of(23, 0))) {
            if (home.isHeatingAllowed() || isFireplaceWorking()) {
                if (room.getTemperatureSensor().getTemperature() < 20.5 && !room.isHeatingActive()) {
                    room.setHeatingActive(true);
                    airShellyProClient.controlBathDown(true).subscribe();
                    log.info(
                        "Bathroom down heating is active, current temperature is [{}C]",
                        room.getTemperatureSensor().getTemperature()
                    );
                } else if (room.getTemperatureSensor().getTemperature() > 20.5 && room.isHeatingActive()) {
                    room.setHeatingActive(false);
                    airShellyProClient.controlBathDown(false).subscribe();
                    log.info(
                        "Bathroom down heating is disabled, current temperature is [{}C]",
                        room.getTemperatureSensor().getTemperature()
                    );
                }
            }
        } else {
            if (room.isHeatingActive()) {
                room.setHeatingActive(false);
                airShellyProClient.controlBathDown(false).subscribe();
            }
        }
    }

    private void updateLivingRoom(final Room room) {
        if (LocalTime.now().isAfter(LocalTime.of(5, 0)) && LocalTime.now().isBefore(LocalTime.of(23, 0))) {
            if (home.isHeatingAllowed() || isFireplaceWorking()) {
                if (room.getTemperatureSensor().getTemperature() < 20.5 && !room.isHeatingActive()) {
                    room.setHeatingActive(true);
                    airShellyProClient.controlLivingRoom(true).subscribe();
                    log.info(
                        "Living Room heating is active, current temperature is [{}C]",
                        room.getTemperatureSensor().getTemperature()
                    );
                } else if (room.getTemperatureSensor().getTemperature() > 20.5 && room.isHeatingActive()) {
                    room.setHeatingActive(false);
                    airShellyProClient.controlLivingRoom(false).subscribe();
                    log.info(
                        "Living Room heating is disabled, current temperature is [{}C]",
                        room.getTemperatureSensor().getTemperature()
                    );
                }
            }
        } else {
            if (room.isHeatingActive()) {
                room.setHeatingActive(false);
                airShellyProClient.controlLivingRoom(false).subscribe();
            }
        }
    }

    private boolean isFireplaceWorking() {
        return home.getBoiler().getHeatingSources().stream()
            .filter(heatingSource -> heatingSource.getType().equals(HeatingSourceType.FIREPLACE))
            .anyMatch(heatingSource -> heatingSource.getTemperature() > 35);
    }
}
