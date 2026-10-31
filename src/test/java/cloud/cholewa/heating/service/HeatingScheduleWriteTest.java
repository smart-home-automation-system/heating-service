package cloud.cholewa.heating.service;

import cloud.cholewa.heating.config.RelayConfig;
import cloud.cholewa.heating.db.model.HeatingScheduleEntity;
import cloud.cholewa.heating.db.repository.HeatingScheduleRepository;
import cloud.cholewa.heating.infrastructure.error.HeatingErrorId;
import cloud.cholewa.heating.infrastructure.error.RoomNotFoundException;
import cloud.cholewa.heating.infrastructure.error.ScheduleException;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.Schedule;
import cloud.cholewa.heating.model.ScheduleBody;
import cloud.cholewa.heating.model.StoredSchedule;
import cloud.cholewa.home.model.RoomName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SUNDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//The writes of HAS-356: checked, stored, and the heaters loaded again before the answer - so the next pass
//of the room heats by what was written. The repository is a fake list behind a mock: what is saved is what
//the next load reads
@ExtendWith(MockitoExtension.class)
class HeatingScheduleWriteTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-31T10:00:00Z"), ZoneId.of("Europe/Warsaw"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 31, 11, 0);
    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 10, 1, 8, 0);

    @Mock
    private HeatingScheduleRepository repository;

    private final List<HeatingScheduleEntity> table = new ArrayList<>();
    private HeaterActor officeRadiator;
    private HeatingScheduleService sut;

    @BeforeEach
    void setUp() {
        officeRadiator = HeaterActor.builder().type(HeaterType.RADIATOR).build();
        final Home home = new Home(List.of(
            Room.builder().name(RoomName.LIVING_ROOM)
                .heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR).build())
                .heaterActor(HeaterActor.builder().type(HeaterType.FLOOR).build())
                .build(),
            Room.builder().name(RoomName.OFFICE).heaterActor(officeRadiator).build(),
            Room.builder().name(RoomName.SANCTUM).heaterActor(HeaterActor.builder().type(HeaterType.RADIATOR).build()).build(),
            Room.builder().name(RoomName.LOFT).build()
        ));
        //as in application.yaml: no relay of SANCTUM, whose radiator is switched outside the service
        final RelayConfig relayConfig = new RelayConfig();
        relayConfig.setHeater(Map.of("living_room", 1, "office", 3));
        relayConfig.setFloor(Map.of("living_room", 0));
        sut = new HeatingScheduleService(home, repository, relayConfig, CLOCK);
    }

    @Test
    void should_store_a_new_schedule_switched_on_and_give_it_to_the_heater_before_it_answers() {
        backTheRepositoryWithTheTable();
        table.add(row(4L, "office", "radiator", List.of("MONDAY"), "05:00", "08:00", true));

        sut.create("Office", "radiator", body(List.of("SUNDAY", "MONDAY"), "08:00", "17:00", "21.5"))
            .as(StepVerifier::create)
            .assertNext(created -> assertThat(created).isEqualTo(new StoredSchedule(
                5L, RoomName.OFFICE, HeaterType.RADIATOR, List.of(MONDAY, SUNDAY), LocalTime.of(8, 0), LocalTime.of(17, 0), 21.5, true)))
            .verifyComplete();

        assertThat(table.getLast()).satisfies(stored -> {
            assertThat(stored.room()).isEqualTo("office");
            assertThat(stored.heater()).isEqualTo("radiator");
            assertThat(stored.days()).containsExactly("MONDAY", "SUNDAY");
            assertThat(stored.createdAt()).isEqualTo(NOW);
            assertThat(stored.updatedAt()).isEqualTo(NOW);
        });
        //the heater holds it already: the next pass of the room - within a minute - heats by it
        assertThat(officeRadiator.getSchedules()).extracting(Schedule::getId).containsExactly(4L, 5L);
        assertThat(officeRadiator.getSchedules().getLast().covers(LocalTime.of(10, 0), SUNDAY)).isTrue();
    }

    @Test
    void should_store_the_room_and_the_heater_by_the_names_the_table_keeps() {
        backTheRepositoryWithTheTable();

        sut.create("LIVING ROOM", "Floor", body(List.of("SATURDAY"), "06:00", "22:00", "21"))
            .as(StepVerifier::create)
            .assertNext(created -> assertThat(created.room()).isEqualTo(RoomName.LIVING_ROOM))
            .verifyComplete();

        assertThat(table.getLast().room()).isEqualTo("living room");
        assertThat(table.getLast().heater()).isEqualTo("floor");
    }

    @Test
    void should_refuse_a_room_the_house_does_not_have() {
        sut.create("attic", "radiator", body(List.of("MONDAY"), "08:00", "17:00", "21"))
            .as(StepVerifier::create)
            .verifyError(RoomNotFoundException.class);
    }

    //SANCTUM's radiator is switched outside the service, LOFT has none, and the office has no floor heating
    @Test
    void should_refuse_a_heater_the_room_has_not_or_the_service_does_not_drive() {
        assertHeaterNotFound("sanctum", "radiator");
        assertHeaterNotFound("loft", "radiator");
        assertHeaterNotFound("office", "floor");
        assertHeaterNotFound("office", "towel rail");
        verify(repository, never()).save(any());
    }

    @Test
    void should_refuse_a_schedule_that_overlaps_another_of_the_heater_and_store_nothing() {
        when(repository.findAllByRoomAndHeater("office", "radiator"))
            .thenReturn(Flux.just(row(4L, "office", "radiator", List.of("MONDAY"), "08:00", "17:00", true)));

        sut.create("office", "radiator", body(List.of("MONDAY"), "16:00", "22:00", "21"))
            .as(StepVerifier::create)
            .verifyErrorSatisfies(error -> assertThat(error)
                .isInstanceOfSatisfying(ScheduleException.class, refused -> assertThat(refused.getError()).isEqualTo(HeatingErrorId.INVALID_SCHEDULE))
                .hasMessageContaining("overlaps schedule 4"));
        verify(repository, never()).save(any());
    }

    //the days, the window and the temperature are replaced; whether it is on, and when it was made, stay
    @Test
    void should_replace_a_schedule_and_keep_whether_it_is_switched_on() {
        backTheRepositoryWithTheTable();
        table.add(row(4L, "office", "radiator", List.of("MONDAY"), "08:00", "17:00", false));
        table.add(row(5L, "office", "radiator", List.of("MONDAY"), "17:00", "22:00", true));

        //it may overlap what it was itself, never the other one
        sut.replace(4L, body(List.of("MONDAY"), "07:00", "17:00", "20"))
            .as(StepVerifier::create)
            .assertNext(replaced -> {
                assertThat(replaced.startTime()).isEqualTo(LocalTime.of(7, 0));
                assertThat(replaced.enabled()).isFalse();
            })
            .verifyComplete();

        assertThat(table.getFirst()).satisfies(stored -> {
            assertThat(stored.id()).isEqualTo(4L);
            assertThat(stored.temperature()).isEqualByComparingTo("20");
            assertThat(stored.createdAt()).isEqualTo(CREATED);
            assertThat(stored.updatedAt()).isEqualTo(NOW);
        });
        assertThat(officeRadiator.getSchedules()).extracting(Schedule::getStartTime).containsExactly(LocalTime.of(7, 0), LocalTime.of(17, 0));

        sut.replace(4L, body(List.of("MONDAY"), "07:00", "18:00", "20"))
            .as(StepVerifier::create)
            .verifyErrorMessage("the window overlaps schedule 5 (MONDAY 17:00-22:00)");
    }

    @Test
    void should_answer_not_found_for_a_schedule_that_is_not_stored() {
        when(repository.findById(9L)).thenReturn(Mono.empty());

        sut.replace(9L, body(List.of("MONDAY"), "07:00", "17:00", "20")).as(StepVerifier::create)
            .verifyErrorSatisfies(error -> assertNotFound(error, 9L));
        sut.enable(9L, true).as(StepVerifier::create).verifyErrorSatisfies(error -> assertNotFound(error, 9L));
        sut.remove(9L).as(StepVerifier::create).verifyErrorSatisfies(error -> assertNotFound(error, 9L));
    }

    @Test
    void should_switch_a_schedule_off_and_on_again() {
        backTheRepositoryWithTheTable();
        table.add(row(4L, "office", "radiator", List.of("SATURDAY"), "08:00", "17:00", true));
        final LocalDateTime saturdayNoon = LocalDateTime.of(2026, 10, 31, 12, 0);

        sut.enable(4L, false).as(StepVerifier::create).verifyComplete();
        assertThat(table.getFirst().enabled()).isFalse();
        assertThat(table.getFirst().createdAt()).isEqualTo(CREATED);
        assertThat(officeRadiator.getSchedules().getFirst().covers(saturdayNoon.toLocalTime(), saturdayNoon.getDayOfWeek())).isFalse();

        sut.enable(4L, true).as(StepVerifier::create).verifyComplete();
        assertThat(officeRadiator.getSchedules().getFirst().covers(saturdayNoon.toLocalTime(), saturdayNoon.getDayOfWeek())).isTrue();
    }

    @Test
    void should_remove_a_schedule_from_the_table_and_from_the_heater() {
        backTheRepositoryWithTheTable();
        table.add(row(4L, "office", "radiator", List.of("MONDAY"), "08:00", "17:00", true));
        sut.load().as(StepVerifier::create).verifyComplete();
        assertThat(officeRadiator.getSchedules()).hasSize(1);

        sut.remove(4L).as(StepVerifier::create).verifyComplete();

        assertThat(table).isEmpty();
        assertThat(officeRadiator.getSchedules()).isEmpty();
    }

    //two writes at once could each pass the overlap check and meet: the second starts when the first ended
    @Test
    void should_carry_out_one_write_after_the_other() {
        backTheRepositoryWithTheTable();
        final Sinks.Empty<Void> firstHeld = Sinks.empty();
        final List<HeatingScheduleEntity> saved = new ArrayList<>();
        doAnswer(invocation -> {
            final HeatingScheduleEntity row = invocation.getArgument(0);
            final HeatingScheduleEntity stored = withId(row, table.size() + 1L);
            saved.add(stored);
            table.add(stored);
            //the first save waits until the test lets it go
            return saved.size() == 1 ? firstHeld.asMono().thenReturn(stored) : Mono.just(stored);
        }).when(repository).save(any());

        final List<StoredSchedule> answers = new ArrayList<>();
        sut.create("office", "radiator", body(List.of("MONDAY"), "08:00", "17:00", "21")).subscribe(answers::add);
        sut.create("office", "radiator", body(List.of("MONDAY"), "09:00", "10:00", "21")).subscribe(answers::add, error -> answers.add(null));

        //the second has not even read the other schedules of the heater yet
        verify(repository, times(1)).findAllByRoomAndHeater("office", "radiator");
        firstHeld.tryEmitEmpty();

        //and when it does it sees the first, and is refused
        assertThat(answers).hasSize(2);
        assertThat(answers.getFirst().startTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(answers.getLast()).isNull();
        assertThat(table).hasSize(1);
    }

    //cancelled while it waited (its caller went away), a write must not let the next one past the running one
    @Test
    void should_keep_the_order_when_a_waiting_write_is_cancelled() {
        backTheRepositoryWithTheTable();
        final Sinks.Empty<Void> firstHeld = Sinks.empty();
        final List<HeatingScheduleEntity> saved = new ArrayList<>();
        doAnswer(invocation -> {
            final HeatingScheduleEntity stored = withId(invocation.getArgument(0), saved.size() + 1L);
            saved.add(stored);
            return saved.size() == 1 ? firstHeld.asMono().thenReturn(stored) : Mono.just(stored);
        }).when(repository).save(any());

        sut.create("office", "radiator", body(List.of("MONDAY"), "08:00", "17:00", "21")).subscribe();
        sut.create("office", "radiator", body(List.of("TUESDAY"), "08:00", "17:00", "21")).subscribe().dispose();
        sut.create("office", "radiator", body(List.of("WEDNESDAY"), "08:00", "17:00", "21")).subscribe();

        //the third waits for the first, which is still saving
        assertThat(saved).hasSize(1);
        firstHeld.tryEmitEmpty();
        assertThat(saved).extracting(HeatingScheduleEntity::days).containsExactly(List.of("MONDAY"), List.of("WEDNESDAY"));
    }

    //a write that failed does not hold up the next one
    @Test
    void should_carry_out_a_write_after_one_that_failed() {
        backTheRepositoryWithTheTable();

        sut.create("office", "radiator", body(List.of(), "08:00", "17:00", "21")).as(StepVerifier::create).verifyError(ScheduleException.class);
        sut.create("office", "radiator", body(List.of("MONDAY"), "08:00", "17:00", "21")).as(StepVerifier::create).expectNextCount(1).verifyComplete();
    }

    //a write hung on the database must not hold every later one for good
    @Test
    void should_end_a_hung_write_at_its_bound_and_carry_out_the_next() {
        backTheRepositoryWithTheTable();
        doAnswer(invocation -> Mono.never()).doAnswer(invocation -> Mono.just(withId(invocation.getArgument(0), 1L)))
            .when(repository).save(any());

        StepVerifier.withVirtualTime(() -> sut.create("office", "radiator", body(List.of("MONDAY"), "08:00", "17:00", "21")))
            .expectSubscription()
            .thenAwait(HeatingScheduleService.WRITE_TIMEOUT)
            .verifyError(TimeoutException.class);

        sut.create("office", "radiator", body(List.of("SUNDAY"), "08:00", "17:00", "21"))
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
    }

    //the row is stored, the heaters could not be loaded: the caller is told, and reads the rooms again
    @Test
    void should_fail_a_write_whose_schedules_could_not_be_loaded_again() {
        when(repository.findAllByRoomAndHeater("office", "radiator")).thenReturn(Flux.empty());
        when(repository.save(any())).thenAnswer(invocation -> Mono.just(withId(invocation.getArgument(0), 1L)));
        when(repository.findAllByOrderByIdAsc()).thenReturn(Flux.error(new IllegalStateException("database down")));

        sut.create("office", "radiator", body(List.of("MONDAY"), "08:00", "17:00", "21"))
            .as(StepVerifier::create)
            .verifyErrorMessage("database down");

        final ArgumentCaptor<HeatingScheduleEntity> stored = ArgumentCaptor.forClass(HeatingScheduleEntity.class);
        verify(repository).save(stored.capture());
        assertThat(stored.getValue().enabled()).isTrue();
    }

    private void assertHeaterNotFound(final String room, final String heater) {
        sut.create(room, heater, body(List.of("MONDAY"), "08:00", "17:00", "21"))
            .as(StepVerifier::create)
            .verifyErrorSatisfies(error -> assertThat(error)
                .isInstanceOfSatisfying(ScheduleException.class, refused -> assertThat(refused.getError()).isEqualTo(HeatingErrorId.NOT_FOUND_HEATER)));
    }

    private static void assertNotFound(final Throwable error, final long id) {
        assertThat(error)
            .isInstanceOfSatisfying(ScheduleException.class, refused -> assertThat(refused.getError()).isEqualTo(HeatingErrorId.NOT_FOUND_SCHEDULE))
            .hasMessage("no schedule with id " + id);
    }

    //the mock answers from the list, the way the table would
    private void backTheRepositoryWithTheTable() {
        lenient().when(repository.findAllByOrderByIdAsc()).thenAnswer(invocation -> Flux.fromIterable(List.copyOf(table)));
        lenient().when(repository.findAllByRoomAndHeater(any(), any())).thenAnswer(invocation -> Flux.fromIterable(table.stream()
            .filter(row -> row.room().equals(invocation.getArgument(0)) && row.heater().equals(invocation.getArgument(1)))
            .toList()));
        lenient().when(repository.findById(any(Long.class))).thenAnswer(invocation -> Mono.justOrEmpty(table.stream()
            .filter(row -> row.id().equals(invocation.getArgument(0)))
            .findFirst()));
        lenient().when(repository.save(any())).thenAnswer(invocation -> {
            final HeatingScheduleEntity row = invocation.getArgument(0);
            if (row.id() == null) {
                final long next = table.stream().mapToLong(HeatingScheduleEntity::id).max().orElse(0) + 1;
                final HeatingScheduleEntity stored = withId(row, next);
                table.add(stored);
                return Mono.just(stored);
            }
            table.replaceAll(existing -> existing.id().equals(row.id()) ? row : existing);
            return Mono.just(row);
        });
        lenient().when(repository.delete(any())).thenAnswer(invocation -> {
            final HeatingScheduleEntity row = invocation.getArgument(0);
            table.removeIf(existing -> existing.id().equals(row.id()));
            return Mono.empty();
        });
    }

    private static HeatingScheduleEntity withId(final HeatingScheduleEntity row, final long id) {
        return new HeatingScheduleEntity(id, row.room(), row.heater(), row.days(), row.startTime(), row.endTime(),
            row.temperature(), row.enabled(), row.createdAt(), row.updatedAt());
    }

    private static HeatingScheduleEntity row(final Long id, final String room, final String heater, final List<String> days,
                                             final String from, final String to, final boolean enabled) {
        return new HeatingScheduleEntity(id, room, heater, days, LocalTime.parse(from), LocalTime.parse(to),
            new BigDecimal("21.0"), enabled, CREATED, CREATED);
    }

    private static ScheduleBody body(final List<String> days, final String from, final String to, final String temperature) {
        return new ScheduleBody(days, from, to, new BigDecimal(temperature));
    }
}
