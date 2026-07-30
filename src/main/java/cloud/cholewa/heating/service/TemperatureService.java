package cloud.cholewa.heating.service;

import cloud.cholewa.heating.db.repository.TemperatureRepository;
import cloud.cholewa.heating.mapper.TemperatureMapper;
import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.home.model.TemperatureMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemperatureService {

    private static final long SAVE_RETRY_ATTEMPTS = 2;
    private static final Duration SAVE_RETRY_BACKOFF = Duration.ofMillis(200);

    private final TemperatureMapper temperatureMapper;
    private final TemperatureRepository temperatureRepository;
    private final HomeService homeService;

    public Mono<Void> handleTemperature(final TemperatureMessage message) {
        return Mono.fromCallable(() -> temperatureMapper.toEntity(message))
            //retry the write only - retrying the whole chain would resend the relay commands
            .flatMap(entity -> temperatureRepository.save(entity)
                .retryWhen(Retry.backoff(SAVE_RETRY_ATTEMPTS, SAVE_RETRY_BACKOFF)
                    .filter(TransientDataAccessException.class::isInstance)))
            .doOnNext(entity ->
                log.info("Saved temperature: {}°C for room: {}", entity.temperature(), entity.room()))
            .flatMap(entity ->
                homeService.processRoomTemperature(RoomName.fromValue(entity.room()), entity.temperature()));
    }
}
