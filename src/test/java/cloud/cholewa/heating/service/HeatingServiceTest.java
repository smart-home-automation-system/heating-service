package cloud.cholewa.heating.service;

import cloud.cholewa.heating.client.ShellyClient;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.HomeStatus;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.shelly.model.ShellyPro4StatusResponse;
import cloud.cholewa.shelly.model.ShellyProRelayResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static cloud.cholewa.heating.model.HeaterType.FLOOR;
import static cloud.cholewa.heating.model.HeaterType.RADIATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeatingServiceTest {

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private Clock clock;
    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private ShellyClient shellyClient;
    @Spy
    private HomeStatus homeStatus;

    @InjectMocks
    HeatingService sut;

    @Test
    void should_return_system_active_state() {
        when(homeStatus.isEnabledHomeHeatingSystem()).thenReturn(true);
        when(homeStatus.isAnyHeaterActive()).thenReturn(true);

        sut.queryHeatingSystemEnabledAndActive()
            .as(StepVerifier::create)
            .assertNext(reply -> assertThat(reply.getActive()).isTrue())
            .verifyComplete();

        verify(homeStatus).isEnabledHomeHeatingSystem();
        verifyNoMoreInteractions(homeStatus, shellyClient);
    }

    @Test
    void should_enable_room_heating_when_actor_not_working_for_and_schedule_active() {
        homeStatus.setEnabledHomeHeatingSystem(true);

        final Room room = Room.builder()
            .name(RoomName.BEDROOM)
            .heaterActor(HeaterActor.builder().type(RADIATOR).working(false).inSchedule(true).build())
            .build();

        mockClock();

        when(shellyClient.getHeaterActorStatus(any(), any()))
            .thenReturn(Mono.just(ShellyPro4StatusResponse.builder().output(false).build()));

        when(shellyClient.controlHeaterActor(any(), any(), anyBoolean()))
            .thenReturn(Mono.just(ShellyProRelayResponse.builder().ison(true).build()));

        sut.processHeatingRequest(room)
            .as(StepVerifier::create)
            .assertNext(result -> assertThat(result.isRoomHeatingEnabled()).isTrue())
            .verifyComplete();

        verify(shellyClient, times(1)).getHeaterActorStatus(any(), any());
        verify(shellyClient, times(1)).controlHeaterActor(any(), any(), eq(true));
        verify(shellyClient, never()).controlHeaterActor(any(), any(), eq(false));
        verifyNoMoreInteractions(shellyClient);
    }

    @Test
    void should_enable_room_heating_when_actor_working_for_and_schedule_active_and_fresh_status() {
        homeStatus.setEnabledHomeHeatingSystem(true);

        final Room room = Room.builder()
            .name(RoomName.BEDROOM)
            .heaterActor(HeaterActor.builder().type(RADIATOR).working(true).lastStatusUpdate(LocalDateTime.of(
                2026,
                1,
                19,
                11,
                56
            )).inSchedule(true).build())
            .build();

        mockClock();

        sut.processHeatingRequest(room)
            .as(StepVerifier::create)
            .assertNext(result -> assertThat(result.isRoomHeatingEnabled()).isTrue())
            .verifyComplete();

        verify(shellyClient, never()).getHeaterActorStatus(any(), any());
        verify(shellyClient, never()).controlHeaterActor(any(), any(), eq(true));
        verify(shellyClient, never()).controlHeaterActor(any(), any(), eq(false));
        verifyNoMoreInteractions(shellyClient);
    }

    @Test
    void should_enable_room_heating_when_actor_not_working_for_and_schedule_active_and_fresh_status() {
        homeStatus.setEnabledHomeHeatingSystem(true);

        final Room room = Room.builder()
            .name(RoomName.BEDROOM)
            .heaterActor(HeaterActor.builder()
                .type(RADIATOR)
                .working(false)
                .lastStatusUpdate(LocalDateTime.of(2026, 1, 19, 11, 56))
                .inSchedule(true)
                .build())
            .build();

        when(shellyClient.controlHeaterActor(any(), any(), anyBoolean()))
            .thenReturn(Mono.just(ShellyProRelayResponse.builder().ison(true).build()));

        mockClock();

        sut.processHeatingRequest(room)
            .as(StepVerifier::create)
            .assertNext(result -> assertThat(result.isRoomHeatingEnabled()).isTrue())
            .verifyComplete();

        verify(shellyClient, never()).getHeaterActorStatus(any(), any());
        verify(shellyClient, times(1)).controlHeaterActor(any(), any(), eq(true));
        verify(shellyClient, never()).controlHeaterActor(any(), any(), eq(false));
        verifyNoMoreInteractions(shellyClient);
    }

    @Test
    void should_enable_room_heating_when_any_actor_working_for_two_heaters_schedule_first_active_second_inactive() {
        homeStatus.setEnabledHomeHeatingSystem(true);

        final Room room = Room.builder()
            .name(RoomName.BEDROOM)
            .heaterActor(HeaterActor.builder().type(RADIATOR).inSchedule(true).working(false).build())
            .heaterActor(HeaterActor.builder().type(FLOOR).inSchedule(false).working(false).build())
            .build();

        mockClock();

        when(shellyClient.getHeaterActorStatus(eq(RADIATOR), any()))
            .thenReturn(Mono.just(ShellyPro4StatusResponse.builder().output(false).build()));

        when(shellyClient.getHeaterActorStatus(eq(FLOOR), any()))
            .thenReturn(Mono.just(ShellyPro4StatusResponse.builder().output(false).build()));

        when(shellyClient.controlHeaterActor(eq(RADIATOR), any(), anyBoolean()))
            .thenReturn(Mono.just(ShellyProRelayResponse.builder().ison(true).build()));

        sut.processHeatingRequest(room)
            .as(StepVerifier::create)
            .assertNext(result -> assertThat(result.isRoomHeatingEnabled()).isTrue())
            .verifyComplete();

        verify(shellyClient, times(1)).getHeaterActorStatus(eq(RADIATOR), any());
        verify(shellyClient, times(1)).getHeaterActorStatus(eq(FLOOR), any());
        verify(shellyClient, times(1)).controlHeaterActor(eq(RADIATOR), any(), eq(true));
        verify(shellyClient, never()).controlHeaterActor(eq(FLOOR), any(), eq(false));
        verifyNoMoreInteractions(shellyClient);
    }

    @Test
    void should_enable_room_heating_when_all_actor_working_for_two_heaters_schedule_all_active() {
        homeStatus.setEnabledHomeHeatingSystem(true);

        final Room room = Room.builder()
            .name(RoomName.BEDROOM)
            .heaterActor(HeaterActor.builder().type(RADIATOR).inSchedule(true).working(false).build())
            .heaterActor(HeaterActor.builder().type(FLOOR).inSchedule(true).working(false).build())
            .build();

        mockClock();

        when(shellyClient.getHeaterActorStatus(eq(RADIATOR), any()))
            .thenReturn(Mono.just(ShellyPro4StatusResponse.builder().output(false).build()));

        when(shellyClient.getHeaterActorStatus(eq(FLOOR), any()))
            .thenReturn(Mono.just(ShellyPro4StatusResponse.builder().output(false).build()));

        when(shellyClient.controlHeaterActor(eq(RADIATOR), any(), anyBoolean()))
            .thenReturn(Mono.just(ShellyProRelayResponse.builder().ison(true).build()));

        when(shellyClient.controlHeaterActor(eq(FLOOR), any(), anyBoolean()))
            .thenReturn(Mono.just(ShellyProRelayResponse.builder().ison(true).build()));

        sut.processHeatingRequest(room)
            .as(StepVerifier::create)
            .assertNext(result -> assertThat(result.isRoomHeatingEnabled()).isTrue())
            .verifyComplete();

        verify(shellyClient, times(1)).getHeaterActorStatus(eq(RADIATOR), any());
        verify(shellyClient, times(1)).getHeaterActorStatus(eq(FLOOR), any());
        verify(shellyClient, times(1)).controlHeaterActor(eq(RADIATOR), any(), eq(true));
        verify(shellyClient, times(1)).controlHeaterActor(eq(FLOOR), any(), eq(true));
        verify(shellyClient, never()).controlHeaterActor(any(), any(), eq(false));
        verifyNoMoreInteractions(shellyClient);
    }

    @Test
    void should_disable_room_heating_when_no_actor_working() {
        homeStatus.setEnabledHomeHeatingSystem(true);

        Room room = Room.builder()
            .name(RoomName.BEDROOM)
            .heaterActor(HeaterActor.builder().type(RADIATOR).inSchedule(false).working(false).build())
            .build();

        mockClock();

        when(shellyClient.getHeaterActorStatus(any(), any()))
            .thenReturn(Mono.just(ShellyPro4StatusResponse.builder().output(false).build()));

        sut.processHeatingRequest(room)
            .as(StepVerifier::create)
            .assertNext(result -> assertThat(result.isRoomHeatingEnabled()).isFalse())
            .verifyComplete();

        verify(shellyClient, times(1)).getHeaterActorStatus(any(), any());
        verify(shellyClient, never()).controlHeaterActor(any(), any(), eq(true));
        verify(shellyClient, never()).controlHeaterActor(any(), any(), eq(false));
        verifyNoMoreInteractions(shellyClient);
    }

    @Test
    void should_refresh_actor_status_when_stale() {
        homeStatus.setEnabledHomeHeatingSystem(true);

        HeaterActor actor = HeaterActor.builder()
            .type(RADIATOR)
            .lastStatusUpdate(LocalDateTime.of(2026, 1, 19, 11, 50)) // 10 minutes ago
            .inSchedule(true)
            .working(false)
            .build();

        mockClock(); // 2026-01-19 12:00

        Room room = Room.builder().name(RoomName.BEDROOM).heaterActor(actor).build();

        when(shellyClient.getHeaterActorStatus(any(), any()))
            .thenReturn(Mono.just(ShellyPro4StatusResponse.builder().output(false).build()));

        when(shellyClient.controlHeaterActor(any(), any(), anyBoolean()))
            .thenReturn(Mono.just(ShellyProRelayResponse.builder().ison(true).build()));

        sut.processHeatingRequest(room)
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(actor.getLastStatusUpdate()).isEqualTo(LocalDateTime.of(2026, 1, 19, 12, 0));
                assertThat(actor.isWorking()).isTrue();
            })
            .verifyComplete();

        verify(shellyClient).getHeaterActorStatus(RADIATOR, RoomName.BEDROOM);
    }

    @Test
    void should_not_refresh_actor_status_when_fresh() {
        homeStatus.setEnabledHomeHeatingSystem(true);

        HeaterActor actor = HeaterActor.builder()
            .type(RADIATOR)
            .lastStatusUpdate(LocalDateTime.of(2026, 1, 19, 11, 58)) // 2 minutes ago
            .inSchedule(true)
            .working(false) // Not working but in schedule
            .build();

        Room room = Room.builder().name(RoomName.BEDROOM).heaterActor(actor).build();

        mockClock(); // 2026-01-19 12:00

        when(shellyClient.controlHeaterActor(any(), any(), anyBoolean()))
            .thenReturn(Mono.just(ShellyProRelayResponse.builder().ison(true).build()));

        sut.processHeatingRequest(room)
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(actor.getLastStatusUpdate()).isEqualTo(LocalDateTime.of(2026, 1, 19, 12, 0));
                assertThat(result.isRoomHeatingEnabled()).isTrue();
            })
            .verifyComplete();

        verify(shellyClient).controlHeaterActor(RADIATOR, RoomName.BEDROOM, true);
        verify(shellyClient, never()).getHeaterActorStatus(any(), any());
    }

    @Test
    void should_turn_off_actor_when_system_disabled_and_output_on() {
        homeStatus.setEnabledHomeHeatingSystem(false);

        HeaterActor actor = HeaterActor.builder().type(RADIATOR).inSchedule(true).working(true).build();

        Room room = Room.builder().name(RoomName.BEDROOM).heaterActor(actor).build();

        mockClock();

        when(shellyClient.getHeaterActorStatus(any(), any()))
            .thenReturn(Mono.just(ShellyPro4StatusResponse.builder().output(true).build()));

        when(shellyClient.controlHeaterActor(any(), any(), anyBoolean()))
            .thenReturn(Mono.just(ShellyProRelayResponse.builder().ison(false).build()));

        sut.processHeatingRequest(room)
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.isRoomHeatingEnabled()).isFalse();
                assertThat(actor.isWorking()).isFalse();
            })
            .verifyComplete();

        verify(shellyClient).getHeaterActorStatus(RADIATOR, RoomName.BEDROOM);
        verify(shellyClient).controlHeaterActor(RADIATOR, RoomName.BEDROOM, false);
        verifyNoMoreInteractions(shellyClient);
    }

    @Test
    void should_turn_off_actor_when_out_of_schedule_and_output_on() {
        homeStatus.setEnabledHomeHeatingSystem(true);

        HeaterActor actor = HeaterActor.builder().type(RADIATOR).inSchedule(false).working(true).build();

        Room room = Room.builder().name(RoomName.BEDROOM).heaterActor(actor).build();

        mockClock();

        when(shellyClient.getHeaterActorStatus(any(), any()))
            .thenReturn(Mono.just(ShellyPro4StatusResponse.builder().output(true).build()));

        when(shellyClient.controlHeaterActor(any(), any(), anyBoolean()))
            .thenReturn(Mono.just(ShellyProRelayResponse.builder().ison(false).build()));

        sut.processHeatingRequest(room)
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.isRoomHeatingEnabled()).isFalse();
                assertThat(actor.isWorking()).isFalse();
            })
            .verifyComplete();

        verify(shellyClient).getHeaterActorStatus(RADIATOR, RoomName.BEDROOM);
        verify(shellyClient).controlHeaterActor(RADIATOR, RoomName.BEDROOM, false);
        verifyNoMoreInteractions(shellyClient);
    }

    @Test
    void should_control_actor_when_output_differs_from_expected() {
        homeStatus.setEnabledHomeHeatingSystem(true);

        HeaterActor actor = HeaterActor.builder().type(RADIATOR).inSchedule(true)
            .working(false) // expected not working
            .build();

        Room room = Room.builder().name(RoomName.BEDROOM).heaterActor(actor).build();

        mockClock();

        when(shellyClient.getHeaterActorStatus(any(), any()))
            .thenReturn(Mono.just(ShellyPro4StatusResponse.builder().output(false).build())); // but output is on

        when(shellyClient.controlHeaterActor(any(), any(), anyBoolean()))
            .thenReturn(Mono.just(ShellyProRelayResponse.builder().ison(true).build()));

        sut.processHeatingRequest(room)
            .as(StepVerifier::create)
            .assertNext(result -> assertThat(actor.isWorking()).isTrue())
            .verifyComplete();

        verify(shellyClient).getHeaterActorStatus(RADIATOR, RoomName.BEDROOM);
        verify(shellyClient).controlHeaterActor(RADIATOR, RoomName.BEDROOM, true);
        verifyNoMoreInteractions(shellyClient);
    }

    @Test
    void should_update_actor_status_from_relay_response() {
        homeStatus.setEnabledHomeHeatingSystem(true);

        HeaterActor actor = HeaterActor.builder().type(RADIATOR).inSchedule(true).working(false).build();

        Room room = Room.builder().name(RoomName.BEDROOM).heaterActor(actor).build();

        mockClock();

        when(shellyClient.getHeaterActorStatus(any(), any()))
            .thenReturn(Mono.just(ShellyPro4StatusResponse.builder().output(false).build()));

        when(shellyClient.controlHeaterActor(any(), any(), anyBoolean()))
            .thenReturn(Mono.just(ShellyProRelayResponse.builder().ison(true).build()));

        sut.processHeatingRequest(room)
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(actor.isWorking()).isTrue();
                assertThat(actor.getLastStatusUpdate()).isNotNull();
            })
            .verifyComplete();
    }

    private void mockClock() {
        when(clock.instant())
            .thenReturn(LocalDateTime.of(2026, 1, 19, 12, 0).atZone(ZoneId.systemDefault()).toInstant());

        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    }
}
