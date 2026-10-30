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
import reactor.util.context.ContextView;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeatingStatusService {

    //the bound of the pass after the switch - that of the pass of every minute (DecisionScheduler)
    static final Duration DECISION_LIMIT = Duration.ofSeconds(50);

    private final HomeStatus homeStatus;
    private final HeatingStatusRepository heatingStatusRepository;
    private final HeatingStatusMapper heatingStatusMapper;
    private final HomeService homeService;

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
            .flatMap(entity -> Mono.deferContextual(context -> {
                decideRooms(context);
                return Mono.just(entity);
            }))
            .map(heatingStatusMapper::toReply)
            .doOnError(ex -> log.error("Error while updating heating status: {}", ex.getMessage()))
            .onErrorMap(throwable -> new HeatingException("Failed to update heating status: " + throwable.getMessage()));
    }

    private void updateHeatingStatus(final HeatingStatusEntity entity) {
        log.info("Updated heating status to: {}", entity.status());
        homeStatus.setEnabledHomeHeatingSystem(entity.status());
        homeStatus.setHomeHeatingSystemUpdatedAt(entity.date());
    }

    //HAS-352: every room decided at once, not with its next reading - on 2026-10-30 two rooms started nine
    //minutes after the switch, and switched off the relays stayed on. Detached from the reply: the pass calls
    //relays (seconds each when one is slow) and a caller waits for the switch only - the dashboard reads the
    //rooms again. Bounded as the pass of every minute; a failure is logged and never reaches the reply. It
    //carries the context of the request, so its lines keep the traceId of the switch
    private void decideRooms(final ContextView context) {
        homeService.decideRooms()
            .timeout(DECISION_LIMIT)
            .doOnError(throwable -> log.error("Error deciding the rooms after the switch of the heating", throwable))
            .onErrorComplete()
            .contextWrite(context)
            .subscribe();
    }
}
