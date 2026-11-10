package cloud.cholewa.heating.scheduler;

import cloud.cholewa.commons.scheduling.ScheduledPass;
import cloud.cholewa.heating.config.RetentionProperties;
import cloud.cholewa.heating.db.repository.HeatingStatusRepository;
import cloud.cholewa.heating.db.repository.TemperatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

//Every night at 03:35 of the house the room temperatures and the changes of the house switch older than
//heating.retention (730 days) are deleted (HAS-413), never the latest change of the switch - it is the state of the
//house. One statement a table, one after the other, each with its own WARN: a failed delete is tried again the next
//night and never keeps the other table. "Now" is read inside the chain; bounded at LIMIT
@Slf4j
@Component
@RequiredArgsConstructor
public class HeatingRetention {

    static final Duration LIMIT = Duration.ofMinutes(5);

    private final TemperatureRepository temperatureRepository;
    private final HeatingStatusRepository heatingStatusRepository;
    private final RetentionProperties properties;
    private final Clock clock;

    @Scheduled(cron = "0 35 3 * * *")
    Mono<Void> purge() {
        return ScheduledPass.bounded("heating retention", LIMIT, () -> Mono.defer(this::purgeOnce));
    }

    private Mono<Void> purgeOnce() {
        final LocalDateTime before = LocalDateTime.now(clock).minus(properties.retention());
        return purge("room temperatures", temperatureRepository.deleteBefore(before), before)
            .then(Mono.defer(() -> purge("heating switch changes",
                heatingStatusRepository.deleteBeforeKeepingTheLatest(before), before)));
    }

    private static Mono<Void> purge(final String what, final Mono<Long> delete, final LocalDateTime before) {
        return delete
            .doOnNext(deleted -> log.info("The {} before {} deleted: {}", what, before, deleted))
            .then()
            .onErrorResume(failure -> Mono.fromRunnable(() ->
                log.warn("The {} before {} could not be deleted: {}", what, before, failure.toString())));
    }
}
