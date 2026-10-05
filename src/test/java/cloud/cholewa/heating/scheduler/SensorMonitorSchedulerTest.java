package cloud.cholewa.heating.scheduler;

import cloud.cholewa.heating.service.SensorMonitorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorMonitorSchedulerTest {

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private SensorMonitorService sensorMonitorService;

    @InjectMocks
    private SensorMonitorScheduler sut;

    @Test
    void should_complete_when_check_succeeds() {
        when(sensorMonitorService.checkSensors()).thenReturn(Mono.empty());

        sut.checkSensors().as(StepVerifier::create).verifyComplete();
    }

    //an error signal would only be logged by Spring as an unexpected one; the pass ends quietly here
    @Test
    void should_complete_when_check_fails() {
        when(sensorMonitorService.checkSensors()).thenReturn(Mono.error(new IllegalStateException("database down")));

        sut.checkSensors().as(StepVerifier::create).verifyComplete();
    }
}
