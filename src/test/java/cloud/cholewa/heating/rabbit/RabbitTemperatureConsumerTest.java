package cloud.cholewa.heating.rabbit;

import cloud.cholewa.heating.service.TemperatureService;
import cloud.cholewa.home.model.TemperatureMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitTemperatureConsumerTest {

    @Mock
    private TemperatureService temperatureService;

    @InjectMocks
    private RabbitTemperatureConsumer sut;

    @Test
    void should_consume_temperature_message() {
        when(temperatureService.handleTemperature(any())).thenReturn(Mono.empty());

        sut.consumeTemperature(new TemperatureMessage())
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    void should_not_fail_when_service_fails() {
        when(temperatureService.handleTemperature(any())).thenReturn(Mono.error(new RuntimeException("Async Fail")));

        sut.consumeTemperature(new TemperatureMessage())
            .as(StepVerifier::create)
            .verifyComplete();
    }
}
