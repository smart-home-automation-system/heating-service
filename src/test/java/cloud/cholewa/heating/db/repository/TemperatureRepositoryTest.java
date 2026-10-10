package cloud.cholewa.heating.db.repository;

import cloud.cholewa.heating.db.model.TemperatureBucket;
import cloud.cholewa.heating.db.model.TemperatureEntity;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.TimeZone;

//The one test that runs SQL: the history query is PostgreSQL's own, so it runs against a
//PostgreSQL in Docker (Testcontainers), on the tables the migrations of the service create.
//The slice builds a plain connection from spring.r2dbc.*, not the pooled one of cholewa-commons,
//which is why the container needs no SSL here. Without a Docker the test fails, it is not skipped.
@DataR2dbcTest
@Testcontainers
@ActiveProfiles("test")
class TemperatureRepositoryTest {

    private static final String ROOM = "living room";
    private static final long TWENTY_MINUTES = 1200;
    private static final long ONE_HOUR = 3600;
    private static final long THREE_HOURS = 10800;

    private static final LocalDateTime MIDNIGHT = LocalDateTime.of(2026, 10, 8, 0, 0);
    private static final LocalDateTime NEXT_MIDNIGHT = MIDNIGHT.plusDays(1);

    private static final TimeZone ZONE_OF_THE_BUILD = TimeZone.getDefault();

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private TemperatureRepository sut;

    @DynamicPropertySource
    static void connectToTheContainer(final DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://%s:%d/%s".formatted(
            POSTGRES.getHost(), POSTGRES.getMappedPort(5432), POSTGRES.getDatabaseName()));
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
    }

    //The zone: the image runs on the zone of the house and a build server on UTC, where a
    //date-time converted on its way to the database would come back unharmed - set before the
    //context opens its first connection. The migrations: the test profile switches Flyway off
    //and the slice would not run it anyway
    @BeforeAll
    static void liveOnTheClockOfTheHouseAndMigrate() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Warsaw"));

        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .load()
            .migrate();
    }

    @AfterAll
    static void giveTheZoneBack() {
        TimeZone.setDefault(ZONE_OF_THE_BUILD);
    }

    @BeforeEach
    void forgetTheReadings() {
        sut.deleteAll().as(StepVerifier::create).verifyComplete();
    }

    @Test
    void should_average_the_readings_of_a_bucket_and_answer_the_buckets_oldest_first() {
        save(ROOM, MIDNIGHT.plusMinutes(20), 20.0);
        save(ROOM, MIDNIGHT.plusMinutes(5), 21.0);
        save(ROOM, MIDNIGHT.plusMinutes(10), 22.0);
        save(ROOM, MIDNIGHT.plusMinutes(19).plusSeconds(59), 23.0);

        sut.findHistory(ROOM, MIDNIGHT, NEXT_MIDNIGHT, TWENTY_MINUTES).as(StepVerifier::create)
            .expectNext(new TemperatureBucket(MIDNIGHT, 22.0))
            .expectNext(new TemperatureBucket(MIDNIGHT.plusMinutes(20), 20.0))
            .verifyComplete();
    }

    @Test
    void should_answer_no_row_for_a_bucket_without_a_reading() {
        save(ROOM, MIDNIGHT.plusMinutes(1), 21.0);
        save(ROOM, MIDNIGHT.plusMinutes(61), 19.0);

        sut.findHistory(ROOM, MIDNIGHT, NEXT_MIDNIGHT, TWENTY_MINUTES).as(StepVerifier::create)
            .expectNext(new TemperatureBucket(MIDNIGHT, 21.0))
            .expectNext(new TemperatureBucket(MIDNIGHT.plusHours(1), 19.0))
            .verifyComplete();
    }

    @Test
    void should_answer_nothing_when_the_room_has_no_reading_in_the_range() {
        save(ROOM, MIDNIGHT.minusDays(3), 21.0);

        sut.findHistory(ROOM, MIDNIGHT, NEXT_MIDNIGHT, TWENTY_MINUTES).as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    void should_include_the_start_of_the_range_and_not_its_end() {
        save(ROOM, MIDNIGHT.minusSeconds(1), 10.0);
        save(ROOM, MIDNIGHT, 21.0);
        save(ROOM, NEXT_MIDNIGHT.minusSeconds(1), 22.0);
        save(ROOM, NEXT_MIDNIGHT, 30.0);

        sut.findHistory(ROOM, MIDNIGHT, NEXT_MIDNIGHT, TWENTY_MINUTES).as(StepVerifier::create)
            .expectNext(new TemperatureBucket(MIDNIGHT, 21.0))
            .expectNext(new TemperatureBucket(NEXT_MIDNIGHT.minusMinutes(20), 22.0))
            .verifyComplete();
    }

    @Test
    void should_leave_the_readings_of_another_room_out() {
        save(ROOM, MIDNIGHT.plusMinutes(5), 21.0);
        save("cinema", MIDNIGHT.plusMinutes(6), 12.0);

        sut.findHistory(ROOM, MIDNIGHT, NEXT_MIDNIGHT, TWENTY_MINUTES).as(StepVerifier::create)
            .expectNext(new TemperatureBucket(MIDNIGHT, 21.0))
            .verifyComplete();
    }

    @Test
    void should_start_a_bucket_of_an_hour_on_the_full_hour() {
        save(ROOM, MIDNIGHT.plusHours(22).plusMinutes(47), 21.0);

        sut.findHistory(ROOM, MIDNIGHT, NEXT_MIDNIGHT, ONE_HOUR).as(StepVerifier::create)
            .expectNext(new TemperatureBucket(MIDNIGHT.plusHours(22), 21.0))
            .verifyComplete();
    }

    @Test
    void should_start_a_bucket_of_three_hours_on_the_clock_of_the_house() {
        save(ROOM, MIDNIGHT.plusHours(2).plusMinutes(59), 20.0);
        save(ROOM, MIDNIGHT.plusHours(22).plusMinutes(47), 21.0);

        sut.findHistory(ROOM, MIDNIGHT, NEXT_MIDNIGHT, THREE_HOURS).as(StepVerifier::create)
            .expectNext(new TemperatureBucket(MIDNIGHT, 20.0))
            .expectNext(new TemperatureBucket(MIDNIGHT.plusHours(21), 21.0))
            .verifyComplete();
    }

    //the buckets are aligned to the clock, not to the range: the first one starts before "from"
    //and averages only what lies within the range
    @Test
    void should_start_the_first_bucket_before_the_range_when_the_range_starts_inside_it() {
        save(ROOM, MIDNIGHT.plusMinutes(5), 10.0);
        save(ROOM, MIDNIGHT.plusMinutes(15), 21.0);

        sut.findHistory(ROOM, MIDNIGHT.plusMinutes(10), NEXT_MIDNIGHT, TWENTY_MINUTES).as(StepVerifier::create)
            .expectNext(new TemperatureBucket(MIDNIGHT, 21.0))
            .verifyComplete();
    }

    @Test
    void should_average_into_the_last_bucket_only_what_lies_before_the_end_of_the_range() {
        save(ROOM, MIDNIGHT.plusMinutes(5), 21.0);
        save(ROOM, MIDNIGHT.plusMinutes(15), 30.0);

        sut.findHistory(ROOM, MIDNIGHT.minusHours(1), MIDNIGHT.plusMinutes(10), TWENTY_MINUTES)
            .as(StepVerifier::create)
            .expectNext(new TemperatureBucket(MIDNIGHT, 21.0))
            .verifyComplete();
    }

    //a reading every 10 minutes over 2 days that start at 00:10: the 144 buckets of two days and
    //the one the range starts inside of
    @Test
    void should_answer_one_bucket_more_when_the_range_starts_inside_a_bucket() {
        final LocalDateTime from = MIDNIGHT.plusMinutes(10);

        Flux.range(0, 288)
            .map(index -> new TemperatureEntity(null, from.plusMinutes(10L * index), ROOM, 21.0))
            .as(sut::saveAll)
            .as(StepVerifier::create)
            .expectNextCount(288)
            .verifyComplete();

        sut.findHistory(ROOM, from, from.plusDays(2), TWENTY_MINUTES).as(StepVerifier::create)
            .expectNext(new TemperatureBucket(MIDNIGHT, 21.0))
            .expectNextCount(143)
            .expectNext(new TemperatureBucket(MIDNIGHT.plusDays(2), 21.0))
            .verifyComplete();
    }

    @Test
    void should_keep_the_buckets_on_the_clock_of_the_house_on_every_day_of_a_long_range() {
        save(ROOM, MIDNIGHT.plusDays(1).plusMinutes(1), 20.0);
        save(ROOM, MIDNIGHT.plusDays(6).plusHours(22).plusMinutes(47), 21.0);

        sut.findHistory(ROOM, MIDNIGHT, MIDNIGHT.plusDays(8), THREE_HOURS).as(StepVerifier::create)
            .expectNext(new TemperatureBucket(MIDNIGHT.plusDays(1), 20.0))
            .expectNext(new TemperatureBucket(MIDNIGHT.plusDays(6).plusHours(21), 21.0))
            .verifyComplete();
    }

    //Wall-clock times, stored and asked for as they are: 02:30 of the night the summer time
    //begins is an hour the clock of the house skips, and a conversion would move it to 03:30
    @Test
    void should_not_move_a_reading_of_the_hour_the_clock_skips_when_the_summer_time_begins() {
        final LocalDateTime night = LocalDateTime.of(2026, 3, 29, 0, 0);
        save(ROOM, night.plusHours(2).plusMinutes(30), 21.0);

        sut.findHistory(ROOM, night, night.plusDays(1), TWENTY_MINUTES).as(StepVerifier::create)
            .expectNext(new TemperatureBucket(night.plusHours(2).plusMinutes(20), 21.0))
            .verifyComplete();
    }

    //the hour that happens twice has one set of buckets: both passes are averaged into them
    @Test
    void should_average_the_hour_the_clock_repeats_into_the_same_buckets_when_the_summer_time_ends() {
        final LocalDateTime night = LocalDateTime.of(2026, 10, 25, 0, 0);
        save(ROOM, night.plusHours(2).plusMinutes(30), 21.0);
        save(ROOM, night.plusHours(2).plusMinutes(35), 23.0);
        save(ROOM, night.plusHours(3).plusMinutes(1), 19.0);

        sut.findHistory(ROOM, night, night.plusDays(1), ONE_HOUR).as(StepVerifier::create)
            .expectNext(new TemperatureBucket(night.plusHours(2), 22.0))
            .expectNext(new TemperatureBucket(night.plusHours(3), 19.0))
            .verifyComplete();
    }

    @Test
    void should_round_an_average_to_two_decimals() {
        save(ROOM, MIDNIGHT.plusMinutes(1), 20.0);
        save(ROOM, MIDNIGHT.plusMinutes(2), 20.01);
        save(ROOM, MIDNIGHT.plusMinutes(3), 20.01);
        save(ROOM, MIDNIGHT.plusMinutes(21), 20.0);
        save(ROOM, MIDNIGHT.plusMinutes(22), 20.0);
        save(ROOM, MIDNIGHT.plusMinutes(23), 20.01);

        sut.findHistory(ROOM, MIDNIGHT, NEXT_MIDNIGHT, TWENTY_MINUTES).as(StepVerifier::create)
            .expectNext(new TemperatureBucket(MIDNIGHT, 20.01))
            .expectNext(new TemperatureBucket(MIDNIGHT.plusMinutes(20), 20.0))
            .verifyComplete();
    }

    private void save(final String room, final LocalDateTime date, final double temperature) {
        sut.save(new TemperatureEntity(null, date, room, temperature)).as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
    }
}
