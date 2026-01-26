package cloud.cholewa.heating.service;

import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.Schedule;
import cloud.cholewa.heating.model.Temperature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private Clock clock;

    @InjectMocks
    private ScheduleService sut;

    @Test
    void should_process_schedule_when_has_no_heaters() {
        final Room testRoom = Room.builder().build();

        sut.processSchedule(testRoom)
            .as(StepVerifier::create)
            .assertNext(room -> {
                assertThat(room).isNotNull();
                assertThat(room.getHeaterActors()).isEmpty();
            })
            .verifyComplete();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("rooms_with_heaters")
    void should_process_schedule_when_has_heaters(
        final String name,
        final Room testRoom,
        final LocalDateTime dateTime,
        final Boolean... expectedStates
    ) {
        when(clock.instant()).thenReturn(dateTime.atZone(ZoneId.systemDefault()).toInstant());

        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        sut.processSchedule(testRoom)
            .as(StepVerifier::create)
            .assertNext(room -> {
                assertThat(room).isNotNull();
                assertThat(room.getHeaterActors()).isNotEmpty();
                assertThat(room.getHeaterActors()).extracting(HeaterActor::isInSchedule).containsExactly(expectedStates);
            })
            .verifyComplete();
    }

    private static Stream<Arguments> rooms_with_heaters() {
        return Stream.of(
            Arguments.of(
                "one heater actor, and one schedule with lower temperature",
                testedRoom(
                    List.of(testedHeaterActor(List.of(
                        testedSchedule(LocalTime.of(8, 0), LocalTime.of(18, 0), Set.of(MONDAY), 20.0)))),
                    Temperature.builder().value(16.6).build()
                ),
                LocalDateTime.of(2026, 1, 19, 12, 0),
                new Boolean[]{true}
            ),
            Arguments.of(
                "one heater actor, and one schedule with higher temperature",
                testedRoom(
                    List.of(testedHeaterActor(List.of(
                        testedSchedule(LocalTime.of(8, 0), LocalTime.of(18, 0), Set.of(MONDAY), 20.0)))),
                    Temperature.builder().value(21.6).build()
                ),
                LocalDateTime.of(2026, 1, 19, 12, 0),
                new Boolean[]{false}
            ),
            Arguments.of(
                "one heater actor, and two schedules",
                testedRoom(
                    List.of(testedHeaterActor(List.of(
                        testedSchedule(LocalTime.of(8, 0), LocalTime.of(18, 0), Set.of(MONDAY), 19.0),
                        testedSchedule(LocalTime.of(18, 0), LocalTime.of(22, 0), Set.of(MONDAY), 20.0)
                    ))), Temperature.builder().value(16.6).build()
                ),
                LocalDateTime.of(2026, 1, 19, 12, 0),
                new Boolean[]{true}
            ),
            Arguments.of(
                "two heater actors, different schedules",
                testedRoom(
                    List.of(
                        testedHeaterActor(List.of(testedSchedule(
                            LocalTime.of(8, 0),
                            LocalTime.of(18, 0),
                            Set.of(SUNDAY, MONDAY, THURSDAY),
                            20.0
                        ))),
                        testedHeaterActor(List.of(testedSchedule(
                            LocalTime.of(8, 0),
                            LocalTime.of(18, 0),
                            Set.of(MONDAY),
                            15.0
                        )))
                    ), Temperature.builder().value(16.6).build()
                ),
                LocalDateTime.of(2026, 1, 19, 12, 0),
                new Boolean[]{true, false}
            ),
            Arguments.of(
                "two heater actors, different schedules, lower temperature but time out of schedules",
                testedRoom(
                    List.of(
                        testedHeaterActor(List.of(testedSchedule(
                            LocalTime.of(8, 0),
                            LocalTime.of(18, 0),
                            Set.of(SUNDAY, MONDAY, THURSDAY),
                            20.0
                        ))),
                        testedHeaterActor(List.of(testedSchedule(
                            LocalTime.of(18, 0),
                            LocalTime.of(22, 0),
                            Set.of(MONDAY),
                            21.0
                        )))
                    ), Temperature.builder().value(16.6).build()
                ),
                LocalDateTime.of(2026, 1, 19, 23, 0),
                new Boolean[]{false, false}
            )
        );
    }

    private static Room testedRoom(final List<HeaterActor> heaterActors, final Temperature temperature) {
        return Room.builder()
            .temperature(temperature)
            .heaterActors(heaterActors)
            .build();
    }

    private static HeaterActor testedHeaterActor(final List<Schedule> schedules) {
        return HeaterActor.builder()
            .schedules(schedules)
            .build();
    }

    private static Schedule testedSchedule(
        final LocalTime start,
        final LocalTime end,
        final Set<DayOfWeek> days,
        final double temperature
    ) {
        return Schedule.builder()
            .startTime(start)
            .endTime(end)
            .days(days)
            .temperature(temperature)
            .build();
    }
}
