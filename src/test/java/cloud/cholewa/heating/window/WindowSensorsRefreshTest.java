package cloud.cholewa.heating.window;

import cloud.cholewa.heating.thermometer.ThermometerReadings;
import cloud.cholewa.heating.thermometer.Thermometers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WindowSensorsRefreshTest {

    private final WindowSensors windowSensors = mock(WindowSensors.class);
    private final OpeningSensorMonitor openingSensorMonitor = mock(OpeningSensorMonitor.class);
    private final StuckOpenMonitor stuckOpenMonitor = mock(StuckOpenMonitor.class);
    private final Thermometers thermometers = mock(Thermometers.class);
    private final ThermometerReadings thermometerReadings = mock(ThermometerReadings.class);
    private final WindowSensorsRefresh sut = new WindowSensorsRefresh(windowSensors, openingSensorMonitor,
        stuckOpenMonitor, thermometers, thermometerReadings);

    @BeforeEach
    void setUp() {
        when(thermometers.refresh()).thenReturn(Mono.empty());
        when(thermometerReadings.fromGateways(any())).thenReturn(Mono.empty());
        when(stuckOpenMonitor.check()).thenReturn(Mono.empty());
    }

    //HAS-350: the sensors that may be stuck open are checked after the silent ones
    @Test
    void should_check_the_stuck_open_sensors_after_the_silent_ones() {
        when(windowSensors.refresh()).thenReturn(Mono.empty());
        when(openingSensorMonitor.check()).thenReturn(Mono.empty());

        sut.refresh().as(StepVerifier::create).verifyComplete();

        final org.mockito.InOrder order = org.mockito.Mockito.inOrder(openingSensorMonitor, stuckOpenMonitor);
        order.verify(openingSensorMonitor).check();
        order.verify(stuckOpenMonitor).check();
    }

    //HAS-360: the thermometers are read with the window sensors, also when the windows failed
    @Test
    void should_read_the_thermometers_with_the_window_sensors() {
        when(windowSensors.refresh()).thenReturn(Mono.error(new IllegalStateException("registry down")));
        when(openingSensorMonitor.check()).thenReturn(Mono.empty());

        sut.refresh().as(StepVerifier::create).verifyComplete();

        verify(thermometers).refresh();
    }

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

    //HAS-364: the values of the BLU thermometers the gateways answered, after the read of the gateways
    @Test
    void should_hand_the_components_the_gateways_answered_to_the_thermometers() {
        final java.util.Map<String, java.util.List<cloud.cholewa.shelly.model.ShellyComponent>> read =
            java.util.Map.of("shellydimmerg3-0892724f4e30", java.util.List.of());
        when(windowSensors.refresh()).thenReturn(Mono.empty());
        when(windowSensors.gatewayComponents()).thenReturn(read);
        when(openingSensorMonitor.check()).thenReturn(Mono.empty());

        sut.refresh().as(StepVerifier::create).verifyComplete();

        verify(thermometerReadings).fromGateways(read);
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
