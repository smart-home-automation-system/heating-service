package cloud.cholewa.heating.db.repository;

import cloud.cholewa.heating.db.model.HeatingStatusEntity;
import org.flywaydb.core.Flyway;
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
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HAS-413, the retention of the changes of the house switch, run on a PostgreSQL in Docker against the tables of the
 * migrations as in TemperatureRepositoryTest. The latest change is the state of the switch and is never deleted,
 * however old it is.
 */
@DataR2dbcTest
@Testcontainers
@ActiveProfiles("test")
class HeatingStatusRepositoryTest {

    private static final LocalDateTime BOUND = LocalDateTime.of(2024, 11, 11, 3, 35);

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private HeatingStatusRepository sut;

    @DynamicPropertySource
    static void connectToTheContainer(final DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://%s:%d/%s".formatted(
            POSTGRES.getHost(), POSTGRES.getMappedPort(5432), POSTGRES.getDatabaseName()));
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
    }

    @BeforeAll
    static void migrate() {
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .load()
            .migrate();
    }

    @BeforeEach
    void forgetTheChanges() {
        sut.deleteAll().as(StepVerifier::create).verifyComplete();
    }

    @Test
    void should_delete_the_changes_before_the_bound_and_keep_the_newer() {
        save(BOUND.minusDays(10), true);
        save(BOUND.minusDays(5), false);
        save(BOUND, true);
        save(BOUND.plusDays(1), false);

        sut.deleteBeforeKeepingTheLatest(BOUND).as(StepVerifier::create).expectNext(2L).verifyComplete();

        sut.findAll().map(HeatingStatusEntity::date).collectList().as(StepVerifier::create)
            .assertNext(left -> assertThat(left).containsExactlyInAnyOrder(BOUND, BOUND.plusDays(1)))
            .verifyComplete();
    }

    //the switch untouched for longer than the retention: its one change is what the service loads at the start
    @Test
    void should_keep_the_latest_change_however_old() {
        save(BOUND.minusDays(900), true);
        save(BOUND.minusDays(800), false);

        sut.deleteBeforeKeepingTheLatest(BOUND).as(StepVerifier::create).expectNext(1L).verifyComplete();

        sut.findFirstByOrderByDateDesc().map(HeatingStatusEntity::date).as(StepVerifier::create)
            .expectNext(BOUND.minusDays(800))
            .verifyComplete();
    }

    //two changes at the same time: one survives, the later written (the higher id)
    @Test
    void should_keep_the_latest_of_two_changes_at_the_same_time() {
        save(BOUND.minusDays(800), true);
        save(BOUND.minusDays(800), false);

        sut.deleteBeforeKeepingTheLatest(BOUND).as(StepVerifier::create).expectNext(1L).verifyComplete();

        sut.findAll().map(HeatingStatusEntity::status).collectList().as(StepVerifier::create)
            .assertNext(left -> assertThat(left).containsExactly(false))
            .verifyComplete();
    }

    @Test
    void should_delete_nothing_from_an_empty_table() {
        sut.deleteBeforeKeepingTheLatest(BOUND).as(StepVerifier::create).expectNext(0L).verifyComplete();
    }

    private void save(final LocalDateTime date, final boolean status) {
        sut.save(new HeatingStatusEntity(null, date, status)).as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
    }
}
