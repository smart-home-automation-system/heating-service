package cloud.cholewa.heating.service;

import cloud.cholewa.heating.db.model.HeatingStatusEntity;
import cloud.cholewa.heating.db.repository.HeatingStatusRepository;
import cloud.cholewa.heating.infrastructure.error.HeatingException;
import cloud.cholewa.heating.mapper.HeatingStatusMapper;
import cloud.cholewa.heating.model.HeatingStatusReply;
import cloud.cholewa.heating.model.HomeStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeatingStatusService {

    private final HomeStatus homeStatus;
    private final HeatingStatusRepository heatingStatusRepository;
    private final HeatingStatusMapper heatingStatusMapper;

    public Mono<HeatingStatusReply> getHeatingStatusEnabled() {
        return Mono.just(
            new HeatingStatusReply(
                homeStatus.isEnabledHomeHeatingSystem(),
                homeStatus.getHomeHeatingSystemUpdatedAt()
            ));
    }

    public Mono<HeatingStatusReply> updateHeatingStatusEnabled(final String turn) {
        return Mono.just(turn)
            .map(value -> value.equalsIgnoreCase("on"))
            .flatMap(isEnabled -> heatingStatusRepository.save(heatingStatusMapper.toEntity(isEnabled)))
            .doOnNext(this::updateHeatingStatus)
            .map(heatingStatusMapper::toReply)
            .doOnError(ex -> log.error("Error while updating heating status: {}", ex.getMessage()))
            .onErrorMap(throwable -> new HeatingException("Failed to update heating status: " + throwable.getMessage()));
    }

    private void updateHeatingStatus(final HeatingStatusEntity entity) {
        log.info("Updated heating status to: {}", entity.status());
        homeStatus.setEnabledHomeHeatingSystem(entity.status());
        homeStatus.setHomeHeatingSystemUpdatedAt(entity.date());
    }
}
