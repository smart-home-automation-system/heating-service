package cloud.cholewa.heating.scheduler;

import cloud.cholewa.heating.service.HomeService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DecisionSchedulerTest {

    private final HomeService homeService = mock(HomeService.class);
    private final DecisionScheduler sut = new DecisionScheduler(homeService);

    @Test
    void should_complete_when_the_pass_fails() {
        when(homeService.decideRooms()).thenReturn(Mono.error(new IllegalStateException("relay down")));

        sut.decideRooms().as(StepVerifier::create).verifyComplete();
    }

    //HAS-303: a pass that hangs ends at its bound, below the minute between passes
    @Test
    void should_end_a_pass_that_never_completes_at_its_bound() {
        when(homeService.decideRooms()).thenReturn(Mono.never());

        StepVerifier.withVirtualTime(sut::decideRooms)
            .expectSubscription()
            .expectNoEvent(DecisionScheduler.LIMIT.minusSeconds(1))
            .thenAwait(Duration.ofSeconds(1))
            .verifyComplete();
    }

    //Spring calls the method once and subscribes to the same Mono for every run
    @Test
    void should_build_the_pass_anew_for_every_run() {
        when(homeService.decideRooms()).thenReturn(Mono.empty());

        final Mono<Void> scheduled = sut.decideRooms();
        scheduled.as(StepVerifier::create).verifyComplete();
        scheduled.as(StepVerifier::create).verifyComplete();

        verify(homeService, times(2)).decideRooms();
    }
}
