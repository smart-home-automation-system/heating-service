//package cloud.cholewa.heating.air.service;
//
//import cloud.cholewa.heating.air.client.AirShellyProClient;
//import cloud.cholewa.heating.model.HeatingSourceType;
//import cloud.cholewa.heating.model.Home;
//import cloud.cholewa.heating.model.Pump;
//import cloud.cholewa.heating.model.PumpType;
//import cloud.cholewa.heating.model.Room;
//import cloud.cholewa.home.model.RoomName;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalTime;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class AirHeatingService {
//
//    private final Home home;
//    private final AirShellyProClient airShellyProClient;
//
//    public void controlHeaterSource(final Room room) {
//
//        switch (room.getName()) {
//            case LIVING_ROOM -> updateLivingRoom(room);
//            case BATHROOM_DOWN -> updateBathRoomDown(room);
//            case CINEMA -> updateCinema(room);
//            case BATHROOM_UP -> updateBathRoomUp(room);
//            case BEDROOM -> updateBedroom(room);
//            case LIVIA -> updateLivia(room);
//            case TOBI -> updateTobi(room);
//            case OFFICE -> updateOffice(room);
//            case WARDROBE -> updateWardrobe(room);
//        }
//    }
//
//    private void updateWardrobe(final Room room) {
//        if (LocalTime.now().isAfter(LocalTime.of(5, 0)) && LocalTime.now().isBefore(LocalTime.of(23, 0))) {
//            if (home.isHeatingAllowed() || isFireplaceWorking()) {
//                if (room.getTemperatureSensor().getTemperature() < 19.5 && !room.isHeatingActive()) {
//                    room.setHeatingActive(true);
//                    airShellyProClient.controlFloorFloorWardrobe(true).subscribe();
//                    logStatus(room);
//                } else if (room.getTemperatureSensor().getTemperature() > 19.5 && room.isHeatingActive()) {
//                    room.setHeatingActive(false);
//                    airShellyProClient.controlFloorFloorWardrobe(false).subscribe();
//                    logStatus(room);
//                }
//            }
//        } else {
//            if (room.isHeatingActive()) {
//                room.setHeatingActive(false);
//                airShellyProClient.controlFloorFloorWardrobe(false).subscribe();
//            }
//        }
//        controlFloorPump();
//    }
//
//    private void updateOffice(final Room room) {
//        if (LocalTime.now().isAfter(LocalTime.of(5, 0)) && LocalTime.now().isBefore(LocalTime.of(23, 0))) {
//            if (home.isHeatingAllowed() || isFireplaceWorking()) {
//                if (room.getTemperatureSensor().getTemperature() < 20.5 && !room.isHeatingActive()) {
//                    room.setHeatingActive(true);
//                    airShellyProClient.controlOffice(true).subscribe();
//                    logStatus(room);
//                } else if (room.getTemperatureSensor().getTemperature() > 20.5 && room.isHeatingActive()) {
//                    room.setHeatingActive(false);
//                    airShellyProClient.controlOffice(false).subscribe();
//                    logStatus(room);
//                }
//            }
//        } else {
//            if (room.isHeatingActive()) {
//                room.setHeatingActive(false);
//                airShellyProClient.controlOffice(false).subscribe();
//            }
//        }
//    }
//
//    private void updateTobi(final Room room) {
//        if (LocalTime.now().isAfter(LocalTime.of(5, 0)) && LocalTime.now().isBefore(LocalTime.of(23, 0))) {
//            if (home.isHeatingAllowed() || isFireplaceWorking()) {
//                if (room.getTemperatureSensor().getTemperature() < 20.5 && !room.isHeatingActive()) {
//                    room.setHeatingActive(true);
//                    airShellyProClient.controlTobi(true).subscribe();
//                    logStatus(room);
//                } else if (room.getTemperatureSensor().getTemperature() > 20.5 && room.isHeatingActive()) {
//                    room.setHeatingActive(false);
//                    airShellyProClient.controlTobi(false).subscribe();
//                    logStatus(room);
//                }
//            }
//        } else {
//            if (room.isHeatingActive()) {
//                room.setHeatingActive(false);
//                airShellyProClient.controlTobi(false).subscribe();
//            }
//        }
//    }
//
//    private void updateLivia(final Room room) {
//        if (LocalTime.now().isAfter(LocalTime.of(5, 0)) && LocalTime.now().isBefore(LocalTime.of(23, 0))) {
//            if (home.isHeatingAllowed() || isFireplaceWorking()) {
//                if (room.getTemperatureSensor().getTemperature() < 20.5 && !room.isHeatingActive()) {
//                    room.setHeatingActive(true);
//                    airShellyProClient.controlLivia(true).subscribe();
//                    logStatus(room);
//                } else if (room.getTemperatureSensor().getTemperature() > 20.5 && room.isHeatingActive()) {
//                    room.setHeatingActive(false);
//                    airShellyProClient.controlLivia(false).subscribe();
//                    logStatus(room);
//                }
//            }
//        } else {
//            if (room.isHeatingActive()) {
//                room.setHeatingActive(false);
//                airShellyProClient.controlLivia(false).subscribe();
//            }
//        }
//    }
//
//    private void updateBedroom(final Room room) {
//        if (LocalTime.now().isAfter(LocalTime.of(5, 0)) && LocalTime.now().isBefore(LocalTime.of(23, 0))) {
//            if (home.isHeatingAllowed() || isFireplaceWorking()) {
//                if (room.getTemperatureSensor().getTemperature() < 20.5 && !room.isHeatingActive()) {
//                    room.setHeatingActive(true);
//                    airShellyProClient.controlBedroom(true).subscribe();
//                    logStatus(room);
//                } else if (room.getTemperatureSensor().getTemperature() > 20.5 && room.isHeatingActive()) {
//                    room.setHeatingActive(false);
//                    airShellyProClient.controlBedroom(false).subscribe();
//                    logStatus(room);
//                }
//            }
//        } else {
//            if (room.isHeatingActive()) {
//                room.setHeatingActive(false);
//                airShellyProClient.controlBedroom(false).subscribe();
//            }
//        }
//    }
//
//    private void updateBathRoomUp(final Room room) {
//        if (LocalTime.now().isAfter(LocalTime.of(5, 0)) && LocalTime.now().isBefore(LocalTime.of(23, 0))) {
//            if (home.isHeatingAllowed() || isFireplaceWorking()) {
//                if (room.getTemperatureSensor().getTemperature() < 20.5 && !room.isHeatingActive()) {
//                    room.setHeatingActive(true);
//                    airShellyProClient.controlFloorBathUp(true).subscribe();
//                    logStatus(room);
//                } else if (room.getTemperatureSensor().getTemperature() > 20.5 && room.isHeatingActive()) {
//                    room.setHeatingActive(false);
//                    airShellyProClient.controlFloorBathUp(false).subscribe();
//                    logStatus(room);
//                }
//            }
//        } else {
//            if (room.isHeatingActive()) {
//                room.setHeatingActive(false);
//                airShellyProClient.controlFloorBathUp(false).subscribe();
//            }
//        }
//        controlFloorPump();
//    }
//
//    private void updateCinema(final Room room) {
//        if (LocalTime.now().isAfter(LocalTime.of(5, 0)) && LocalTime.now().isBefore(LocalTime.of(23, 0))) {
//            if (home.isHeatingAllowed() || isFireplaceWorking()) {
//                if (room.getTemperatureSensor().getTemperature() < 20.5 && !room.isHeatingActive()) {
//                    room.setHeatingActive(true);
//                    airShellyProClient.controlCinema(true).subscribe();
//                    logStatus(room);
//                } else if (room.getTemperatureSensor().getTemperature() > 20.5 && room.isHeatingActive()) {
//                    room.setHeatingActive(false);
//                    airShellyProClient.controlCinema(false).subscribe();
//                    logStatus(room);
//                }
//            }
//        } else {
//            if (room.isHeatingActive()) {
//                room.setHeatingActive(false);
//                airShellyProClient.controlCinema(false).subscribe();
//            }
//        }
//    }
//
//    private void updateBathRoomDown(final Room room) {
//        if (LocalTime.now().isAfter(LocalTime.of(5, 0)) && LocalTime.now().isBefore(LocalTime.of(23, 0))) {
//            if (home.isHeatingAllowed() || isFireplaceWorking()) {
//                if (room.getTemperatureSensor().getTemperature() < 20.5 && !room.isHeatingActive()) {
//                    room.setHeatingActive(true);
//                    airShellyProClient.controlFloorBathDown(true).subscribe();
//                    logStatus(room);
//                } else if (room.getTemperatureSensor().getTemperature() > 20.5 && room.isHeatingActive()) {
//                    room.setHeatingActive(false);
//                    airShellyProClient.controlFloorBathDown(false).subscribe();
//                    logStatus(room);
//                }
//            }
//        } else {
//            if (room.isHeatingActive()) {
//                room.setHeatingActive(false);
//                airShellyProClient.controlFloorBathDown(false).subscribe();
//            }
//        }
//    }
//
//    private void updateLivingRoom(final Room room) {
//        if (LocalTime.now().isAfter(LocalTime.of(5, 0)) && LocalTime.now().isBefore(LocalTime.of(23, 0))) {
//            if (home.isHeatingAllowed() || isFireplaceWorking()) {
//                if (room.getTemperatureSensor().getTemperature() < 20.5 && !room.isHeatingActive()) {
//                    room.setHeatingActive(true);
//                    airShellyProClient.controlFloorKitchen(true).subscribe();
//                    logStatus(room);
//                } else if (room.getTemperatureSensor().getTemperature() > 20.5 && room.isHeatingActive()) {
//                    room.setHeatingActive(false);
//                    airShellyProClient.controlFloorKitchen(false).subscribe();
//                    logStatus(room);
//                }
//            }
//        } else {
//            if (room.isHeatingActive()) {
//                room.setHeatingActive(false);
//                airShellyProClient.controlFloorKitchen(false).subscribe();
//            }
//        }
//    }
//
//    private static void logStatus(final Room room) {
//        log.info(
//            "{} heating is disabled, current temperature is [{}C]",
//            room.getName().name(),
//            room.getTemperatureSensor().getTemperature()
//        );
//    }
//
//    private boolean isFireplaceWorking() {
//        return home.getBoiler().getHeatingSources().stream()
//            .filter(heatingSource -> heatingSource.getType().equals(HeatingSourceType.FIREPLACE))
//            .anyMatch(heatingSource -> heatingSource.getTemperature() > 35);
//    }
//
//    private void controlFloorPump() {
//        Pump floorPump = home.getBoiler().getPumps().stream()
//            .filter(pump -> pump.getType().equals(PumpType.FLOOR_PUMP))
//            .findFirst().orElseThrow();
//
//        final boolean isAnyPumpActive = home.getRooms().stream()
//            .filter(room -> (room.getName().equals(RoomName.BATHROOM_UP) || room.getName().equals(RoomName.WARDROBE)))
//            .map(Room::isHeatingActive)
//            .filter(heating -> heating)
//            .anyMatch(aBoolean -> true);
//
//        if (isAnyPumpActive && !floorPump.isRunning()) {
//            floorPump.setRunning(true);
//            airShellyProClient.controlPumpFloor(true).subscribe();
//        } else if (!isAnyPumpActive) {
//            if (floorPump.isRunning()) {
//                floorPump.setRunning(false);
//                airShellyProClient.controlPumpFloor(false).subscribe();
//            }
//        }
//    }
//}
