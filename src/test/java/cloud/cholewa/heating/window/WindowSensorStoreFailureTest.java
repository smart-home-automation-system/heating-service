package cloud.cholewa.heating.window;

import cloud.cholewa.heating.db.repository.WindowSensorReadingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

//HAS-419: nothing of the store may stop what calls it - a write, a read or a delete that fails is a WARN and completes
@ExtendWith(OutputCaptureExtension.class)
class WindowSensorStoreFailureTest {

    private final WindowSensorReadingRepository repository = mock(WindowSensorReadingRepository.class);
    private final WindowSensorStore sut = new WindowSensorStore(repository,
        Clock.fixed(Instant.parse("2026-11-11T21:00:00Z"), ZoneId.of("Europe/Warsaw")));

    @Test
    void should_complete_when_a_write_fails(final CapturedOutput output) {
        when(repository.upsert(any(), any(), any(), any(), any())).thenReturn(Mono.error(new IllegalStateException("pool exhausted")));

        sut.save(67, new WindowSensors.Reading(false, Instant.EPOCH, LocalDateTime.now(), null))
            .as(StepVerifier::create)
            .verifyComplete();

        assertThat(output.getOut()).contains("The reading of window sensor 67 could not be stored, a restart forgets it");
    }

    @Test
    void should_know_none_when_the_read_fails(final CapturedOutput output) {
        when(repository.findAll()).thenReturn(Flux.error(new IllegalStateException("database down")));

        sut.load().as(StepVerifier::create).verifyComplete();

        assertThat(output.getOut()).contains("The stored window sensor readings could not be read, the start knows none");
    }

    @Test
    void should_complete_when_the_delete_fails(final CapturedOutput output) {
        when(repository.deleteOthers(any())).thenReturn(Mono.error(new IllegalStateException("database down")));

        sut.retain(Set.of(67L)).as(StepVerifier::create).verifyComplete();

        assertThat(output.getOut()).contains("The readings of the window sensors gone from the registry could not be deleted");
    }
}
