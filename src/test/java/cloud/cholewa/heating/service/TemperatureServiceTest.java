package cloud.cholewa.heating.service;

import cloud.cholewa.heating.db.model.TemperatureEntity;
import cloud.cholewa.heating.db.repository.TemperatureRepository;
import cloud.cholewa.heating.mapper.TemperatureMapper;
import cloud.cholewa.home.model.TemperatureMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemperatureServiceTest {

    private static final TemperatureEntity TEMPERATURE_ENTITY =
        new TemperatureEntity(1L, LocalDateTime.now(), "cinema", 12.0);

    @Mock
    private TemperatureMapper temperatureMapper;
    @Mock
    private TemperatureRepository temperatureRepository;
    @Mock
    private HomeService homeService;

    @InjectMocks
    private TemperatureService sut;

    @Test
    void should_persist_temperature() {
        when(temperatureMapper.toEntity(any())).thenReturn(TEMPERATURE_ENTITY);

        when(temperatureRepository.save(any())).thenReturn(Mono.just(TEMPERATURE_ENTITY));

        when(homeService.processRoomTemperature(any(), anyDouble())).thenReturn(Mono.empty());

        sut.handleTemperature(new TemperatureMessage())
            .as(StepVerifier::create)
            .verifyComplete();

        verify(temperatureMapper, times(1)).toEntity(any());
        verify(temperatureRepository, times(1)).save(any());

        verifyNoMoreInteractions(temperatureMapper, temperatureRepository);
    }
}
