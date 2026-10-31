package cloud.cholewa.heating.db.repository;

import cloud.cholewa.heating.config.HomeConfig;
import cloud.cholewa.heating.config.RelayConfig;
import cloud.cholewa.heating.db.model.HeatingScheduleEntity;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Schedule;
import cloud.cholewa.heating.service.HeatingScheduleService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

//The schedules move from HomeConfig to the database (HAS-355): the rows V5 inserts, read by the service into
//the heaters of HomeConfig, must be the schedules of 1.12.0 - one by one, in their order. The table is
//PostgreSQL's own (an array of days, its checks), so it runs against a PostgreSQL in Docker, as
//TemperatureRepositoryTest does.
@DataR2dbcTest
@Testcontainers
@ActiveProfiles("test")
@Import({HomeConfig.class, HeatingScheduleService.class, HeatingScheduleRepositoryTest.Binding.class})
class HeatingScheduleRepositoryTest {

    //binds RelayConfig from application.yaml, which the slice would not
    @TestConfiguration
    @EnableConfigurationProperties(RelayConfig.class)
    static class Binding {
    }

    private static final String ALL = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY";
    private static final String WEEKDAYS = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY";
    private static final String WEEKEND = "SATURDAY,SUNDAY";

    //the schedules of HomeConfig of 1.12.0: room, heater, days, from, to, temperature
    private static final List<String> SCHEDULES_OF_1_12_0 = List.of(
        "office radiator " + ALL + " 07:00 23:00 20.5",
        "tobi radiator " + WEEKDAYS + " 05:00 08:00 20.5",
        "tobi radiator " + WEEKDAYS + " 08:00 17:00 19.5",
        "tobi radiator " + WEEKDAYS + " 17:00 22:00 21.0",
        "tobi radiator " + WEEKEND + " 09:00 22:00 21.0",
        "livia radiator " + WEEKDAYS + " 05:00 08:00 21.0",
        "livia radiator " + WEEKDAYS + " 08:00 17:00 20.0",
        "livia radiator " + WEEKDAYS + " 17:00 22:00 21.0",
        "livia radiator " + WEEKEND + " 09:00 22:00 21.0",
        "bedroom radiator " + ALL + " 07:00 08:00 16.0",
        "bedroom radiator " + ALL + " 08:00 21:00 16.0",
        "wardrobe floor " + ALL + " 05:00 20:00 19.5",
        "bathroom_up radiator " + ALL + " 17:00 18:00 22.0",
        "bathroom_up floor " + ALL + " 05:00 09:00 22.5",
        "bathroom_up floor " + ALL + " 17:00 20:00 22.5",
        "living_room radiator " + WEEKDAYS + " 06:00 08:00 21.0",
        "living_room radiator " + WEEKDAYS + " 15:00 22:00 21.0",
        "living_room radiator " + WEEKEND + " 08:00 22:00 21.0",
        "living_room floor " + WEEKDAYS + " 06:00 08:00 21.0",
        "living_room floor " + WEEKDAYS + " 15:00 22:00 21.0",
        "living_room floor " + WEEKEND + " 06:00 22:00 21.0",
        "cinema radiator " + WEEKDAYS + " 05:00 08:00 21.0",
        "cinema radiator " + WEEKDAYS + " 08:00 16:00 20.0",
        "cinema radiator " + WEEKDAYS + " 16:00 22:00 21.0",
        "cinema radiator " + WEEKEND + " 08:00 22:00 21.0",
        "bathroom_down radiator " + ALL + " 16:00 17:00 23.0",
        "bathroom_down floor " + ALL + " 06:00 07:00 22.0",
        "bathroom_down floor " + ALL + " 18:00 20:00 22.0",
        "entrance radiator " + ALL + " 05:00 21:00 16.5",
        "garage radiator " + ALL + " 05:00 20:00 13.0"
    );

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private HeatingScheduleRepository sut;

    @Autowired
    private HeatingScheduleService service;

    @Autowired
    private Home home;

    @Autowired
    private DatabaseClient databaseClient;

    @DynamicPropertySource
    static void connectToTheContainer(final DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://%s:%d/%s".formatted(
            POSTGRES.getHost(), POSTGRES.getMappedPort(5432), POSTGRES.getDatabaseName()));
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
    }

    //the test profile switches Flyway off and the slice would not run it anyway
    @BeforeAll
    static void migrate() {
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .load()
            .migrate();
    }

    @Test
    void should_load_into_the_heaters_exactly_the_schedules_of_home_config_of_1_12_0() {
        service.load().as(StepVerifier::create).verifyComplete();

        final List<String> loaded = new ArrayList<>();
        home.rooms().forEach(room -> room.getHeaterActors().forEach(heater ->
            heater.getSchedules().forEach(schedule -> loaded.add(describe(room.getName().name().toLowerCase(Locale.ROOT), heater, schedule)))));

        assertThat(loaded).containsExactlyInAnyOrderElementsOf(SCHEDULES_OF_1_12_0);
        //and in the order of a heater: the loop heats to the first one the room is still colder than
        assertThat(loaded.stream().filter(line -> line.startsWith("tobi ")).toList())
            .containsExactlyElementsOf(SCHEDULES_OF_1_12_0.stream().filter(line -> line.startsWith("tobi ")).toList());
        assertThat(home.rooms()).flatExtracting(room -> room.getHeaterActors().stream()
                .flatMap(heater -> heater.getSchedules().stream()).toList())
            .allSatisfy(schedule -> {
                assertThat(schedule.isEnabled()).isTrue();
                assertThat(schedule.getId()).isNotNull();
            });
    }

    @Test
    void should_read_the_days_and_the_times_of_a_row_as_stored() {
        sut.findAllByOrderByIdAsc().take(2).collectList().as(StepVerifier::create)
            .assertNext(rows -> {
                assertThat(rows).extracting(HeatingScheduleEntity::room).containsExactly("office", "tobi");
                assertThat(rows.getFirst().days()).hasSize(7).startsWith("MONDAY");
                assertThat(rows.getFirst().createdAt()).isNotNull();
                assertThat(rows.get(1).days()).containsExactly("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
            })
            .verifyComplete();
    }

    //what the write API of the next task will meet: the table refuses what no schedule can be
    @Test
    void should_refuse_a_schedule_that_ends_before_it_starts_or_names_a_day_that_does_not_exist() {
        insert("ARRAY['MONDAY']", "'08:00'", "'07:00'").as(StepVerifier::create).verifyError();
        insert("ARRAY['FUNDAY']", "'07:00'", "'08:00'").as(StepVerifier::create).verifyError();
        insert("ARRAY[]::text[]", "'07:00'", "'08:00'").as(StepVerifier::create).verifyError();
    }

    private Mono<Long> insert(final String days, final String from, final String to) {
        return databaseClient.sql(
                "INSERT INTO heating_schedule (room, heater, days, start_time, end_time, temperature) VALUES ('office', 'radiator', %s, %s, %s, 20)"
                    .formatted(days, from, to))
            .fetch().rowsUpdated();
    }

    private static String describe(final String room, final HeaterActor heater, final Schedule schedule) {
        final String days = Arrays.stream(DayOfWeek.values())
            .filter(schedule.getDays()::contains)
            .map(DayOfWeek::name)
            .collect(Collectors.joining(","));
        //the root locale: a Polish JVM would write 20,5
        return String.format(Locale.ROOT, "%s %s %s %s %s %.1f",
            room, heater.getType(), days, schedule.getStartTime(), schedule.getEndTime(), schedule.getTemperature());
    }
}
