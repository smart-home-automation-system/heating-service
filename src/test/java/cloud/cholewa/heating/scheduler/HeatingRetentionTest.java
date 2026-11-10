package cloud.cholewa.heating.scheduler;

import cloud.cholewa.heating.config.RetentionProperties;
import cloud.cholewa.heating.db.repository.HeatingStatusRepository;
import cloud.cholewa.heating.db.repository.TemperatureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//The nightly retention of the room temperatures and the changes of the house switch (HAS-413)
class HeatingRetentionTest {

    private static final ZoneId HOUSE = ZoneId.of("Europe/Warsaw");
    //03:35 of the house on 11 November
    private static final Instant NIGHT = Instant.parse("2026-11-11T02:35:00Z");
    private static final LocalDateTime BOUND = LocalDateTime.of(2024, 11, 11, 3, 35);

    private final TemperatureRepository temperatures = mock(TemperatureRepository.class);
    private final HeatingStatusRepository statuses = mock(HeatingStatusRepository.class);
    private final AtomicReference<Instant> now = new AtomicReference<>(NIGHT);
    private final HeatingRetention sut = new HeatingRetention(temperatures, statuses,
        new RetentionProperties(Duration.ofDays(730)), new Clock() {
            @Override
            public ZoneId getZone() {
                return HOUSE;
            }

            @Override
            public Clock withZone(final ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now.get();
            }
        });

    @BeforeEach
    void setUp() {
        when(temperatures.deleteBefore(any())).thenReturn(Mono.just(120L));
        when(statuses.deleteBeforeKeepingTheLatest(any())).thenReturn(Mono.just(2L));
    }

    @Test
    void should_delete_both_tables_before_the_retention() {
        sut.purge().as(StepVerifier::create).verifyComplete();

        verify(temperatures).deleteBefore(BOUND);
        verify(statuses).deleteBeforeKeepingTheLatest(BOUND);
    }

    //each table on its own: a failed delete of one never keeps the other, and the pass never fails
    @Test
    void should_delete_the_switch_changes_when_the_temperatures_fail() {
        when(temperatures.deleteBefore(any())).thenReturn(Mono.error(new IllegalStateException("database down")));

        sut.purge().as(StepVerifier::create).verifyComplete();

        verify(statuses).deleteBeforeKeepingTheLatest(BOUND);
    }

    @Test
    void should_complete_when_both_deletes_fail() {
        when(temperatures.deleteBefore(any())).thenReturn(Mono.error(new IllegalStateException("database down")));
        when(statuses.deleteBeforeKeepingTheLatest(any())).thenReturn(Mono.error(new IllegalStateException("database down")));

        sut.purge().as(StepVerifier::create).verifyComplete();
    }

    //Spring subscribes again to the same Mono every night: the bound is read at every pass, not at the start of the pod
    @Test
    void should_move_the_bound_with_every_night() {
        final Mono<Void> pass = sut.purge();

        pass.as(StepVerifier::create).verifyComplete();
        now.set(NIGHT.plus(Duration.ofDays(1)));
        pass.as(StepVerifier::create).verifyComplete();

        verify(temperatures).deleteBefore(BOUND);
        verify(temperatures).deleteBefore(BOUND.plusDays(1));
    }

    @Test
    void should_end_a_pass_that_never_completes_at_its_bound() {
        when(temperatures.deleteBefore(any())).thenReturn(Mono.never());

        StepVerifier.withVirtualTime(sut::purge)
            .thenAwait(HeatingRetention.LIMIT.plusSeconds(1))
            .verifyComplete();
    }

    @Test
    void should_run_at_03_35_every_night() throws NoSuchMethodException {
        final Method purge = HeatingRetention.class.getDeclaredMethod("purge");

        assertThat(purge.getAnnotation(Scheduled.class).cron())
            .isEqualTo("0 35 3 * * *");
    }
}
