package cloud.cholewa.heating.service;

import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.HomeStatus;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.Temperature;
import cloud.cholewa.home.model.RoomName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private Clock clock;
    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private Home home;
    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private HomeStatus homeStatus;
    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private ScheduleService scheduleService;
    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private HeatingService heatingService;
    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private FloorPumpService floorPumpService;

    @InjectMocks
    private HomeService sut;

    @Test
    void should_process_room_temperature_successfully() {
        final Room room = Room.builder()
            .name(RoomName.LIVING_ROOM)
            .temperature(Temperature.builder().build())
            .heaterActor(HeaterActor.builder().build())
            .build();

        mockClock();

        when(home.rooms()).thenReturn(List.of(room));

        when(scheduleService.processSchedule(any())).thenReturn(Mono.just(room));

        when(heatingService.processHeatingRequest(any())).thenReturn(Mono.just(room));

        when(floorPumpService.processFloorPump()).thenReturn(Mono.empty());

        sut.processRoomTemperature(RoomName.LIVING_ROOM, 15)
            .as(StepVerifier::create)
            .verifyComplete();

        verify(home, times(2)).rooms();
        verify(scheduleService).processSchedule(room);
        verify(heatingService).processHeatingRequest(room);
        verify(floorPumpService).processFloorPump();
        verifyNoMoreInteractions(home, scheduleService, heatingService, floorPumpService);

        assertThat(room.getTemperature().getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 19, 12, 0));
        assertThat(room.getTemperature().getValue()).isEqualTo(15);
    }

    @Test
    void should_do_nothing_when_room_not_found() {
        final Room room = Room.builder().name(RoomName.LIVING_ROOM).temperature(Temperature.builder().build()).build();

        when(home.rooms()).thenReturn(Collections.emptyList());

        sut.processRoomTemperature(RoomName.LIVING_ROOM, 15)
            .as(StepVerifier::create)
            .verifyComplete();

        verify(home).rooms();
        verify(scheduleService, never()).processSchedule(room);
        verify(heatingService, never()).processHeatingRequest(room);
        verifyNoMoreInteractions(home, scheduleService, heatingService);
    }

    @Test
    void should_process_only_matching_room_when_multiple_rooms_exist() {
        final Room room1 = Room.builder().name(RoomName.LIVING_ROOM).temperature(Temperature.builder().build()).build();
        final Room room2 = Room.builder().name(RoomName.KITCHEN).temperature(Temperature.builder().build()).build();

        mockClock();

        when(home.rooms()).thenReturn(List.of(room1, room2));

        when(scheduleService.processSchedule(room1)).thenReturn(Mono.just(room1));

        when(heatingService.processHeatingRequest(room1)).thenReturn(Mono.just(room1));

        when(floorPumpService.processFloorPump()).thenReturn(Mono.empty());

        sut.processRoomTemperature(RoomName.LIVING_ROOM, 15)
            .as(StepVerifier::create)
            .verifyComplete();

        verify(home, times(2)).rooms();
        verify(scheduleService).processSchedule(room1);
        verify(heatingService).processHeatingRequest(room1);
        verify(floorPumpService).processFloorPump();
        verifyNoMoreInteractions(home, scheduleService, heatingService, floorPumpService);
    }

    @Test
    void should_handle_error_from_schedule_service() {
        final Room room = Room.builder()
            .name(RoomName.GARAGE)
            .temperature(Temperature.builder().build())
            .heaterActor(HeaterActor.builder().build())
            .build();

        mockClock();

        when(home.rooms()).thenReturn(List.of(room));

        when(scheduleService.processSchedule(any())).thenReturn(Mono.error(new RuntimeException("Error")));

        sut.processRoomTemperature(RoomName.GARAGE, 15)
            .as(StepVerifier::create)
            .verifyComplete();

        verify(home).rooms();
        verify(scheduleService).processSchedule(any());
        verifyNoMoreInteractions(home, scheduleService, heatingService, floorPumpService);
    }

    @Test
    void should_handle_error_from_heating_service() {
        final Room room = Room.builder()
            .name(RoomName.GARAGE)
            .temperature(Temperature.builder().build())
            .heaterActor(HeaterActor.builder().build())
            .build();

        mockClock();

        when(home.rooms()).thenReturn(List.of(room));

        when(scheduleService.processSchedule(any())).thenReturn(Mono.just(room));

        when(heatingService.processHeatingRequest(any())).thenReturn(Mono.error(new RuntimeException("Error")));

        sut.processRoomTemperature(RoomName.GARAGE, 15)
            .as(StepVerifier::create)
            .verifyComplete();

        verify(home).rooms();
        verify(scheduleService).processSchedule(any(Room.class));
        verify(heatingService).processHeatingRequest(any(Room.class));
        verifyNoMoreInteractions(home, scheduleService, heatingService, floorPumpService);
    }

    private void mockClock() {
        when(clock.instant())
            .thenReturn(LocalDateTime.of(2026, 1, 19, 12, 0).atZone(ZoneId.systemDefault()).toInstant());

        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    }
}