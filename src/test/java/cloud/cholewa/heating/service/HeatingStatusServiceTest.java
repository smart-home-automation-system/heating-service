package cloud.cholewa.heating.service;

import cloud.cholewa.heating.db.model.HeatingStatusEntity;
import cloud.cholewa.heating.db.repository.HeatingStatusRepository;
import cloud.cholewa.heating.infrastructure.error.HeatingException;
import cloud.cholewa.heating.mapper.HeatingStatusMapper;
import cloud.cholewa.heating.model.HeatingStatusReply;
import cloud.cholewa.heating.model.HomeStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeatingStatusServiceTest {

    @Mock
    private HeatingStatusRepository heatingStatusRepository;
    @Mock
    private HeatingStatusMapper heatingStatusMapper;
    @Spy
    private HomeStatus homeStatus = new HomeStatus();
    @Mock
    private HomeService homeService;

    @InjectMocks
    private HeatingStatusService sut;

    @Test
    void should_get_heating_status() {
        LocalDateTime now = LocalDateTime.now();
        homeStatus.setEnabledHomeHeatingSystem(true);
        homeStatus.setHomeHeatingSystemUpdatedAt(now);

        sut.getHeatingStatusEnabled()
            .as(StepVerifier::create)
            .assertNext(reply -> {
                assertThat(reply.isHeatingEnabled()).isTrue();
                assertThat(reply.updatedAt()).isEqualTo(now);
            })
            .verifyComplete();
    }

    @Test
    void should_update_heating_status_successfully() {
        String turn = "on";
        LocalDateTime now = LocalDateTime.now();
        HeatingStatusEntity entity = new HeatingStatusEntity(1L, now, true);
        HeatingStatusReply reply = new HeatingStatusReply(true, now);

        when(heatingStatusMapper.toEntity(true)).thenReturn(entity);
        when(heatingStatusRepository.save(any())).thenReturn(Mono.just(entity));
        when(heatingStatusMapper.toReply(entity)).thenReturn(reply);
        when(homeService.decideRooms()).thenReturn(Mono.empty());

        sut.updateHeatingStatusEnabled(turn)
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.isHeatingEnabled()).isTrue();
                assertThat(result.updatedAt()).isEqualTo(now);
            })
            .verifyComplete();

        verify(homeStatus).setEnabledHomeHeatingSystem(true);
        verify(homeStatus).setHomeHeatingSystemUpdatedAt(now);
        verify(heatingStatusRepository).save(entity);
        //HAS-352: every room decided at once, not with its next reading
        verify(homeService).decideRooms();
    }

    //the pass after the switch never reaches the reply: neither its failure nor its length
    @Test
    void should_answer_the_switch_whatever_the_pass_after_it_does() {
        LocalDateTime now = LocalDateTime.now();
        HeatingStatusEntity entity = new HeatingStatusEntity(1L, now, false);
        when(heatingStatusMapper.toEntity(false)).thenReturn(entity);
        when(heatingStatusRepository.save(any())).thenReturn(Mono.just(entity));
        when(heatingStatusMapper.toReply(entity)).thenReturn(new HeatingStatusReply(false, now));

        when(homeService.decideRooms()).thenReturn(Mono.error(new IllegalStateException("relay down")));
        sut.updateHeatingStatusEnabled("off").as(StepVerifier::create).expectNextCount(1).verifyComplete();

        when(homeService.decideRooms()).thenReturn(Mono.never());
        sut.updateHeatingStatusEnabled("off").as(StepVerifier::create).expectNextCount(1).verifyComplete();
    }

    //nothing was switched: nothing is decided
    @Test
    void should_decide_nothing_when_the_switch_was_not_stored() {
        when(heatingStatusMapper.toEntity(false)).thenReturn(new HeatingStatusEntity(null, null, false));
        when(heatingStatusRepository.save(any())).thenReturn(Mono.error(new RuntimeException("DB Error")));

        sut.updateHeatingStatusEnabled("off").as(StepVerifier::create).expectError(HeatingException.class).verify();

        org.mockito.Mockito.verifyNoInteractions(homeService);
    }

    @Test
    void should_handle_error_during_update() {
        String turn = "off";
        when(heatingStatusMapper.toEntity(false)).thenReturn(new HeatingStatusEntity(null, null, false));
        when(heatingStatusRepository.save(any())).thenReturn(Mono.error(new RuntimeException("DB Error")));

        sut.updateHeatingStatusEnabled(turn)
            .as(StepVerifier::create)
            .expectError(HeatingException.class)
            .verify();
    }
}
