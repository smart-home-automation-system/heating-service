package cloud.cholewa.heating.service;

import cloud.cholewa.heating.client.ShellyClient;
import cloud.cholewa.heating.infrastructure.error.BoilerException;
import cloud.cholewa.heating.model.FloorPump;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.shelly.model.ShellyPro4StatusResponse;
import cloud.cholewa.shelly.model.ShellyProRelayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static cloud.cholewa.heating.model.HeaterType.FLOOR;
import static cloud.cholewa.home.model.RoomName.BATHROOM_UP;
import static cloud.cholewa.home.model.RoomName.WARDROBE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FloorPumpServiceTest {

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private Clock clock;
    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private ShellyClient shellyClient;
    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private Home home;

    @InjectMocks
    private FloorPumpService sut;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(
            LocalDateTime.of(2026, 1, 28, 10, 0).atZone(Clock.systemDefaultZone().getZone()).toInstant());

        when(clock.getZone()).thenReturn(Clock.systemDefaultZone().getZone());
    }

    @Test
    void should_refresh_pump_status_when_stale() {
        when(shellyClient.getFloorPumpStatus()).thenReturn(Mono.just(ShellyPro4StatusResponse.builder().output(true).build()));

        when(home.rooms()).thenReturn(Collections.emptyList());

        sut.processFloorPump()
            .as(StepVerifier::create)
            .verifyComplete();

        verify(shellyClient).getFloorPumpStatus();
    }

    @Test
    void should_not_refresh_pump_status_when_fresh() {
        FloorPump floorPump = (FloorPump) ReflectionTestUtils.getField(sut, "floorPump");
        Objects.requireNonNull(floorPump).setUpdatedAt(LocalDateTime.now(clock).minusMinutes(1));

        when(home.rooms()).thenReturn(Collections.emptyList());

        sut.processFloorPump()
            .as(StepVerifier::create)
            .verifyComplete();

        verify(shellyClient, never()).getFloorPumpStatus();
    }

    @Test
    void should_turn_on_floor_pump_when_floor_heaters_are_active() {
        when(shellyClient.getFloorPumpStatus()).thenReturn(Mono.just(ShellyPro4StatusResponse.builder().output(false).build()));

        when(shellyClient.controlFloorPump(anyBoolean()))
            .thenReturn(Mono.just(ShellyProRelayResponse.builder().ison(true).build()));

        when(home.rooms()).thenReturn(
            List.of(Room.builder()
                .name(WARDROBE).heaterActor(HeaterActor.builder().type(FLOOR).working(true).build()).build()));

        sut.processFloorPump()
            .as(StepVerifier::create)
            .verifyComplete();

        verify(shellyClient).getFloorPumpStatus();
        verify(shellyClient).controlFloorPump(true);
        verify(home).rooms();
        verifyNoMoreInteractions(shellyClient, home);

        FloorPump floorPump = (FloorPump) ReflectionTestUtils.getField(sut, "floorPump");
        assertThat(floorPump).isNotNull();
        assertThat(floorPump.isWorking()).isTrue();
        assertThat(floorPump.getUpdatedAt()).isEqualTo(LocalDateTime.now(clock));
    }

    @Test
    void should_not_turn_on_floor_second_time_when_was_activated_first_time_pump_when_floor_heaters_are_active() {
        when(shellyClient.getFloorPumpStatus()).thenReturn(Mono.just(ShellyPro4StatusResponse.builder().output(false).build()));

        when(shellyClient.controlFloorPump(anyBoolean()))
            .thenReturn(Mono.just(ShellyProRelayResponse.builder().ison(true).build()));

        when(home.rooms()).thenReturn(List.of(
            Room.builder().name(WARDROBE).heaterActor(HeaterActor.builder().type(FLOOR).working(true).build()).build()));

        sut.processFloorPump()
            .as(StepVerifier::create)
            .verifyComplete();
        sut.processFloorPump()
            .as(StepVerifier::create)
            .verifyComplete();

        verify(shellyClient).getFloorPumpStatus();
        verify(shellyClient).controlFloorPump(true);
        verify(home, times(2)).rooms();
        verifyNoMoreInteractions(shellyClient, home);

        FloorPump floorPump = (FloorPump) ReflectionTestUtils.getField(sut, "floorPump");
        assertThat(floorPump).isNotNull();
        assertThat(floorPump.isWorking()).isTrue();
        assertThat(floorPump.getUpdatedAt()).isEqualTo(LocalDateTime.now(clock));
    }

    @Test
    void should_turn_off_floor_pump_when_floor_heaters_are_inactive() {
        when(shellyClient.getFloorPumpStatus()).thenReturn(Mono.just(ShellyPro4StatusResponse.builder().output(true).build()));

        when(shellyClient.controlFloorPump(anyBoolean()))
            .thenReturn(Mono.just(ShellyProRelayResponse.builder().ison(false).build()));

        when(home.rooms()).thenReturn(
            List.of(Room.builder()
                .name(BATHROOM_UP).heaterActor(HeaterActor.builder().type(FLOOR).working(false).build()).build()));

        sut.processFloorPump()
            .as(StepVerifier::create)
            .verifyComplete();

        verify(shellyClient).getFloorPumpStatus();
        verify(shellyClient).controlFloorPump(false);
        verify(home).rooms();
        verifyNoMoreInteractions(shellyClient, home);

        FloorPump floorPump = (FloorPump) ReflectionTestUtils.getField(sut, "floorPump");
        assertThat(floorPump).isNotNull();
        assertThat(floorPump.isWorking()).isFalse();
        assertThat(floorPump.getUpdatedAt()).isEqualTo(LocalDateTime.now(clock));
    }

    @Test
    void should_handle_error_when_getting_floor_pump_status() {
        when(shellyClient.getFloorPumpStatus()).thenReturn(Mono.error(new BoilerException("Test exception")));

        sut.processFloorPump()
            .as(StepVerifier::create)
            .verifyErrorSatisfies(throwable -> assertThat(throwable).isInstanceOf(BoilerException.class));

        verify(shellyClient).getFloorPumpStatus();
        verify(home).rooms();
        verifyNoMoreInteractions(shellyClient, home);
    }

    @Test
    void should_handle_error_when_controlling_floor_pump() {
        when(shellyClient.getFloorPumpStatus()).thenReturn(Mono.just(ShellyPro4StatusResponse.builder().output(true).build()));

        when(shellyClient.controlFloorPump(anyBoolean())).thenReturn(Mono.error(new BoilerException("Test exception")));

        when(home.rooms()).thenReturn(List.of(
            Room.builder().name(BATHROOM_UP).heaterActor(HeaterActor.builder().type(FLOOR).working(false).build()).build()));

        sut.processFloorPump()
            .as(StepVerifier::create)
            .verifyErrorSatisfies(throwable -> assertThat(throwable).isInstanceOf(BoilerException.class));

        verify(shellyClient).getFloorPumpStatus();
        verify(home).rooms();
        verify(shellyClient).controlFloorPump(anyBoolean());
        verifyNoMoreInteractions(shellyClient, home);
    }
}