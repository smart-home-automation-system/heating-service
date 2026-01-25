package cloud.cholewa.heating.service;

import cloud.cholewa.heating.mapper.TemperatureMapper;
import cloud.cholewa.heating.db.repository.TemperatureRepository;
import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.home.model.TemperatureMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemperatureService {

    private final TemperatureMapper temperatureMapper;
    private final TemperatureRepository temperatureRepository;
    private final HomeService homeService;
    private final HeatingService heatingService;

    public Mono<Void> handleTemperature(final TemperatureMessage message) {
        return Mono.fromCallable(() -> temperatureMapper.toEntity(message))
            .flatMap(temperatureRepository::save)
            .doOnNext(entity ->
                log.info("Saved temperature: {}C for room {}", entity.temperature(), entity.room()))
            .flatMap(entity ->
                homeService.processRoomTemperature(RoomName.fromValue(entity.room()), entity.temperature()));
    }
}
