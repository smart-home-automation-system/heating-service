package cloud.cholewa.heating.config;

import cloud.cholewa.heating.service.HeatingScheduleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeatingScheduleLoaderTest {

    @Mock
    private HeatingScheduleService service;

    @InjectMocks
    private HeatingScheduleLoader sut;

    @Test
    void should_load_the_schedules_before_the_listeners_start() {
        when(service.load()).thenReturn(Mono.empty());

        sut.start();

        assertThat(sut.isRunning()).isTrue();
        //before the listener containers - a reading must find the schedules - and the web server
        assertThat(sut.getPhase())
            .isLessThan(new RabbitListenerEndpointRegistry().getPhase())
            .isLessThan(Integer.MAX_VALUE - 1024);
    }

    @Test
    void should_try_again_after_a_failed_read() {
        final AtomicInteger attempts = new AtomicInteger();
        when(service.load()).thenReturn(Mono.defer(() -> attempts.incrementAndGet() < 3
            ? Mono.error(new IllegalStateException("database down"))
            : Mono.empty()));

        sut.start();

        assertThat(attempts).hasValue(3);
        assertThat(sut.isRunning()).isTrue();
    }

    //without its schedules the service would hold every room cold: the start fails, the old pod heats on
    @Test
    void should_fail_the_start_when_the_schedules_cannot_be_read() {
        when(service.load()).thenReturn(Mono.error(new IllegalStateException("database down")));

        assertThatThrownBy(sut::start).isInstanceOf(IllegalStateException.class);
        assertThat(sut.isRunning()).isFalse();
    }
}
