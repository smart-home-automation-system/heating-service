package cloud.cholewa.heating.config;

import cloud.cholewa.heating.db.repository.HeatingStatusRepository;
import cloud.cholewa.heating.model.HomeStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class HomeStatusConfig {

    private static final long RETRY_ATTEMPTS = 3;
    private static final Duration RETRY_BACKOFF = Duration.ofMillis(500);
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(30);

    @Bean
    HomeStatus homeStatus() {
        return new HomeStatus();
    }

    @Bean
    @Profile("!test")
    CommandLineRunner initHomeStatus(final HomeStatus homeStatus, final HeatingStatusRepository repository) {
        return args -> {
            log.info("Starting initial home status update from database via CommandLineRunner...");
            repository.findFirstByOrderByDateDesc()
                .doOnNext(entity -> {
                    log.info("Initial home status found in database: {}", entity.status());
                    homeStatus.setEnabledHomeHeatingSystem(entity.status());
                    homeStatus.setHomeHeatingSystemUpdatedAt(entity.date());
                })
                .doOnSuccess(entity -> {
                    if (entity == null) {
                        log.warn("No heating status found in database. Keeping default.");
                    } else {
                        log.info("Initial home status updated successfully.");
                    }
                })
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, RETRY_BACKOFF))
                //log the throwable, not its message: Retry.backoff replaces it with "Retries exhausted"
                //and the actual database error survives only as the cause
                .doOnError(throwable -> log.error("Error updating home status from database", throwable))
                //blocking on purpose: startup must fail loudly instead of serving traffic with heating
                //disabled. The timeout matters - a hung query would leave the pod ready-less forever,
                //with liveness still UP, so kubernetes would never restart it
                .block(STARTUP_TIMEOUT);
        };
    }
}
