package cloud.cholewa.heating.service;

import cloud.cholewa.heating.client.ShellyClient;
import cloud.cholewa.heating.model.FloorPump;
import cloud.cholewa.heating.model.FloorPumpReply;
import cloud.cholewa.heating.model.Home;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

import static org.mockito.Mockito.verifyNoInteractions;

//Apart from FloorPumpServiceTest, whose set-up stubs a clock that reading the state never asks
@ExtendWith(MockitoExtension.class)
class FloorPumpServiceQueryTest {

    private static final LocalDateTime REPORTED_AT = LocalDateTime.of(2026, 10, 8, 18, 31);

    @Mock
    private Clock clock;
    @Mock
    private ShellyClient shellyClient;
    @Mock
    private Home home;

    @InjectMocks
    private FloorPumpService sut;

    @Test
    void should_return_an_empty_state_before_the_pump_has_reported() {
        sut.queryFloorPump()
            .as(StepVerifier::create)
            .expectNext(new FloorPumpReply(null, null))
            .verifyComplete();
    }

    @Test
    void should_return_what_the_pump_last_reported_without_asking_the_device() {
        //built before the state changes: the state is read when the reply is subscribed to
        final Mono<FloorPumpReply> reply = sut.queryFloorPump();

        final FloorPump floorPump = (FloorPump) ReflectionTestUtils.getField(sut, "floorPump");
        Objects.requireNonNull(floorPump).setWorking(true);
        floorPump.setUpdatedAt(REPORTED_AT);

        reply.as(StepVerifier::create)
            .expectNext(new FloorPumpReply(true, REPORTED_AT))
            .verifyComplete();

        verifyNoInteractions(shellyClient, clock, home);
    }

    @Test
    void should_return_a_pump_that_reported_off_as_not_working() {
        final FloorPump floorPump = (FloorPump) ReflectionTestUtils.getField(sut, "floorPump");
        Objects.requireNonNull(floorPump).setUpdatedAt(REPORTED_AT);

        sut.queryFloorPump()
            .as(StepVerifier::create)
            .expectNext(new FloorPumpReply(false, REPORTED_AT))
            .verifyComplete();
    }
}
