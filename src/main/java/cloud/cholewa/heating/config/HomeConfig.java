package cloud.cholewa.heating.config;

import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Humidity;
import cloud.cholewa.heating.model.OpeningSensor;
import cloud.cholewa.heating.model.OpeningType;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.RoomMode;
import cloud.cholewa.heating.model.Schedule;
import cloud.cholewa.heating.model.ScheduleType;
import cloud.cholewa.heating.model.Temperature;
import cloud.cholewa.home.model.RoomName;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;

@Configuration
public class HomeConfig {

    @Bean
    Home home(final List<Room> rooms) {
        return new Home(rooms);
    }

    @Bean
    List<Room> rooms(
        final Room office,
        final Room tobi,
        final Room livia,
        final Room bedroom,
        final Room wardrobe,
        final Room bathroomUp,
        final Room loft,
        final Room livingRoom,
        final Room cinema,
        final Room bathroomDown,
        final Room entrance,
        final Room garage,
        final Room sanctum,
        final Room sauna,
        final Room garden
    ) {
        return List.of(
            office, tobi, livia, bedroom, wardrobe, bathroomUp, loft,
            livingRoom, cinema, bathroomDown, entrance, garage, sanctum, sauna, garden
        );
    }

    @Bean
    Room office() {
        return Room.builder()
            .name(RoomName.OFFICE)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR)
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY))
                    .temperature(20.5)
                    .startTime(LocalTime.of(7, 0))
                    .endTime(LocalTime.of(23, 0))
                    .build())
                .build())
            .openingSensor(OpeningSensor.builder().name(OpeningType.DOOR).build())
            .openingSensor(OpeningSensor.builder().name(OpeningType.WINDOW).build())
            .build();
    }

    @Bean
    Room tobi() {
        return Room.builder()
            .name(RoomName.TOBI)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR)
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
                    .temperature(20.5)
                    .startTime(LocalTime.of(5, 0))
                    .endTime(LocalTime.of(8, 0))
                    .build())
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
                    .temperature(19.5)
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(17, 0))
                    .build())
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
                    .temperature(21)
                    .startTime(LocalTime.of(17, 0))
                    .endTime(LocalTime.of(22, 0))
                    .build())
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(SATURDAY, SUNDAY))
                    .temperature(21)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(22, 0))
                    .build())
                .build())
            .openingSensor(OpeningSensor.builder().name(OpeningType.DOOR).build())
            .openingSensor(OpeningSensor.builder().name(OpeningType.WINDOW).build())
            .build();
    }

    @Bean
    Room livia() {
        return Room.builder()
            .name(RoomName.LIVIA)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR)
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
                    .temperature(21)
                    .startTime(LocalTime.of(5, 0))
                    .endTime(LocalTime.of(8, 0))
                    .build())
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
                    .temperature(20)
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(17, 0))
                    .build())
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
                    .temperature(21)
                    .startTime(LocalTime.of(17, 0))
                    .endTime(LocalTime.of(22, 0))
                    .build())
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(SATURDAY, SUNDAY))
                    .temperature(21)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(22, 0))
                    .build())
                .build())
            .openingSensor(OpeningSensor.builder().name(OpeningType.ROOF).build())
            .openingSensor(OpeningSensor.builder().name(OpeningType.WINDOW).build())
            .build();
    }

    @Bean
    Room bedroom() {
        return Room.builder()
            .name(RoomName.BEDROOM)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR)
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY))
                    .temperature(16)
                    .startTime(LocalTime.of(7, 0))
                    .endTime(LocalTime.of(8, 0))
                    .build())
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY))
                    .temperature(16)
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(21, 0))
                    .build())
                .build())
            .openingSensor(OpeningSensor.builder().name(OpeningType.WINDOW).build())
            .openingSensor(OpeningSensor.builder().name(OpeningType.DOOR).build())
            .openingSensor(OpeningSensor.builder().name(OpeningType.WINDOW2).build())
            .build();
    }

    @Bean
    Room wardrobe() {
        return Room.builder()
            .name(RoomName.WARDROBE)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.FLOOR)
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY))
                    .temperature(19.5)
                    .startTime(LocalTime.of(5, 0))
                    .endTime(LocalTime.of(20, 0))
                    .build())
                .build())
            .openingSensor(OpeningSensor.builder().name(OpeningType.ROOF_LEFT).build())
            .openingSensor(OpeningSensor.builder().name(OpeningType.ROOF_RIGHT).build())
            .build();
    }

    @Bean
    Room bathroomUp() {
        return Room.builder()
            .name(RoomName.BATHROOM_UP)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR)
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY))
                    .temperature(22)
                    .startTime(LocalTime.of(17, 0))
                    .endTime(LocalTime.of(18, 0))
                    .build())
                .build())
            .heaterActor(HeaterActor.builder().type(HeaterType.FLOOR)
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY))
                    .temperature(22.5)
                    .startTime(LocalTime.of(5, 0))
                    .endTime(LocalTime.of(9, 0))
                    .build())
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY))
                    .temperature(22.5)
                    .startTime(LocalTime.of(17, 0))
                    .endTime(LocalTime.of(20, 0))
                    .build())
                .build())
            .openingSensor(OpeningSensor.builder().name(OpeningType.ROOF).build())
            .build();
    }

    @Bean
    Room loft() {
        return Room.builder()
            .name(RoomName.LOFT)
            .temperature(Temperature.builder().build())
            .build();
    }

    @Bean
    Room livingRoom() {
        return Room.builder()
            .name(RoomName.LIVING_ROOM)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR)
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
                    .temperature(21)
                    .startTime(LocalTime.of(6, 0))
                    .endTime(LocalTime.of(8, 0))
                    .build())
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
                    .temperature(21)
                    .startTime(LocalTime.of(15, 0))
                    .endTime(LocalTime.of(22, 0))
                    .build())
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(SATURDAY, SUNDAY))
                    .temperature(21)
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(22, 0))
                    .build())
                .build())
            .heaterActor(HeaterActor.builder().type(HeaterType.FLOOR)
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
                    .temperature(21)
                    .startTime(LocalTime.of(6, 0))
                    .endTime(LocalTime.of(8, 0))
                    .build())
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
                    .temperature(21)
                    .startTime(LocalTime.of(15, 0))
                    .endTime(LocalTime.of(22, 0))
                    .build())
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(SATURDAY, SUNDAY))
                    .temperature(21)
                    .startTime(LocalTime.of(6, 0))
                    .endTime(LocalTime.of(22, 0))
                    .build())
                .build())
            .openingSensor(OpeningSensor.builder().name(OpeningType.DOOR).build())
            .openingSensor(OpeningSensor.builder().name(OpeningType.ENTRANCE_DOOR).build())
            .build();
    }

    @Bean
    Room cinema() {
        return Room.builder()
            .name(RoomName.CINEMA)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR)
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
                    .temperature(21)
                    .startTime(LocalTime.of(5, 0))
                    .endTime(LocalTime.of(8, 0))
                    .build())
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
                    .temperature(20)
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(16, 0))
                    .build())
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
                    .temperature(21)
                    .startTime(LocalTime.of(16, 0))
                    .endTime(LocalTime.of(22, 0))
                    .build())
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(SATURDAY, SUNDAY))
                    .temperature(21)
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(22, 0))
                    .build())
                .build())
            .openingSensor(OpeningSensor.builder().name(OpeningType.WINDOW).build())
            .openingSensor(OpeningSensor.builder().name(OpeningType.WINDOW2).build())
            .build();
    }

    @Bean
    Room bathroomDown() {
        return Room.builder()
            .name(RoomName.BATHROOM_DOWN)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR)
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY))
                    .temperature(23)
                    .startTime(LocalTime.of(17, 0))
                    .endTime(LocalTime.of(18, 0))
                    .build())
                .build())
            .heaterActor(HeaterActor.builder()
                .type(HeaterType.FLOOR)
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY))
                    .temperature(22)
                    .startTime(LocalTime.of(6, 0))
                    .endTime(LocalTime.of(9, 0))
                    .build())
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY))
                    .temperature(22)
                    .startTime(LocalTime.of(17, 0))
                    .endTime(LocalTime.of(20, 0))
                    .build())
                .build())
            .build();
    }

    @Bean
    Room entrance() {
        return Room.builder()
            .name(RoomName.ENTRANCE)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR)
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY))
                    .temperature(16.5)
                    .startTime(LocalTime.of(5, 0))
                    .endTime(LocalTime.of(21, 0))
                    .build())
                .build())
            .openingSensor(OpeningSensor.builder().name(OpeningType.ENTRANCE_DOOR).build())
            .build();
    }

    @Bean
    Room garage() {
        return Room.builder()
            .name(RoomName.GARAGE)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR)
                .schedule(Schedule.builder().type(ScheduleType.HEATING)
                    .days(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY))
                    .temperature(13)
                    .startTime(LocalTime.of(5, 0))
                    .endTime(LocalTime.of(20, 0))
                    .build())
                .build())
            .openingSensor(OpeningSensor.builder().name(OpeningType.GATE_KATE).build())
            .openingSensor(OpeningSensor.builder().name(OpeningType.GATE_KRIS).build())
            .build();
    }

    @Bean
    Room sanctum() {
        return Room.builder()
            .name(RoomName.SANCTUM)
            .mode(RoomMode.HEATING)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR).build())
            .openingSensor(OpeningSensor.builder().name(OpeningType.DOOR).build())
            .build();
    }

    @Bean
    Room sauna() {
        return Room.builder()
            .name(RoomName.SAUNA)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .build();
    }

    @Bean
    Room garden() {
        return Room.builder()
            .name(RoomName.GARDEN)
            .temperature(Temperature.builder().build())
            .humidity(new Humidity())
            .build();
    }
}
