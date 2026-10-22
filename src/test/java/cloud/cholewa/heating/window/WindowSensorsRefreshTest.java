package cloud.cholewa.heating.window;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WindowSensorsRefreshTest {

    private final WindowSensors windowSensors = mock(WindowSensors.class);
    private final OpeningSensorMonitor openingSensorMonitor = mock(OpeningSensorMonitor.class);
    private final WindowSensorsRefresh sut = new WindowSensorsRefresh(windowSensors, openingSensorMonitor);

    @Test
    void should_complete_when_the_read_fails() {
        when(windowSensors.refresh()).thenReturn(Mono.error(new IllegalStateException("registry down")));
        when(openingSensorMonitor.check()).thenReturn(Mono.empty());

        sut.refresh().as(StepVerifier::create).verifyComplete();
    }

    //HAS-303: a read that hangs ends at its bound, below the five minutes between reads, and the
    //check of the silent sensors waits for the next one
    @Test
    void should_end_a_pass_that_never_completes_at_its_bound() {
        final AtomicBoolean checked = new AtomicBoolean();
        when(windowSensors.refresh()).thenReturn(Mono.never());
        when(openingSensorMonitor.check()).thenReturn(Mono.fromRunnable(() -> checked.set(true)));

        StepVerifier.withVirtualTime(sut::refresh)
            .expectSubscription()
            .expectNoEvent(WindowSensorsRefresh.LIMIT.minusSeconds(1))
            .thenAwait(Duration.ofSeconds(1))
            .verifyComplete();

        assertThat(checked).isFalse();
    }

    //Spring calls the method once and subscribes to the same Mono for every run
    @Test
    void should_build_the_pass_anew_for_every_run() {
        when(windowSensors.refresh()).thenReturn(Mono.empty());
        when(openingSensorMonitor.check()).thenReturn(Mono.empty());

        final Mono<Void> scheduled = sut.refresh();
        scheduled.as(StepVerifier::create).verifyComplete();
        scheduled.as(StepVerifier::create).verifyComplete();

        verify(windowSensors, times(2)).refresh();
        verify(openingSensorMonitor, times(2)).check();
    }
}
