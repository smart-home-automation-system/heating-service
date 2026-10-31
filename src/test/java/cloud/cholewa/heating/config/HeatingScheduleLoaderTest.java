package cloud.cholewa.heating.config;

import cloud.cholewa.heating.calendar.HouseCalendar;
import cloud.cholewa.heating.service.HeatingScheduleService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeatingScheduleLoaderTest {

    @Mock
    private HeatingScheduleService service;

    @Mock
    private HouseCalendar houseCalendar;

    @BeforeEach
    void setUp() {
        lenient().when(houseCalendar.refresh()).thenReturn(Mono.empty());
    }

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

    //HAS-358: the type of today right after the schedules, so a reading in the first minute heats a holiday
    //by the weekend schedule; never read when the schedules failed - the start fails there
    @Test
    void should_read_the_calendar_of_today_after_the_schedules() {
        when(service.load()).thenReturn(Mono.empty());

        sut.start();

        final var order = inOrder(service, houseCalendar);
        order.verify(service).load();
        order.verify(houseCalendar).refresh();
    }

    @Test
    void should_not_read_the_calendar_when_the_schedules_fail() {
        when(service.load()).thenReturn(Mono.error(new IllegalStateException("database down")));

        org.assertj.core.api.Assertions.assertThatThrownBy(sut::start).isInstanceOf(IllegalStateException.class);
        verify(houseCalendar, never()).refresh();
    }
}
