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
