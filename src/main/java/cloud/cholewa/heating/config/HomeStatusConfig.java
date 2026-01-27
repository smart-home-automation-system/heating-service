package cloud.cholewa.heating.config;

import cloud.cholewa.heating.db.repository.HeatingStatusRepository;
import cloud.cholewa.heating.model.HomeStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class HomeStatusConfig {

    @Bean
    HomeStatus homeStatus() {
        return new HomeStatus();
    }

    @Bean
    CommandLineRunner initHomeStatus(final HomeStatus homeStatus, final HeatingStatusRepository repository) {
        return args -> {
            log.info("Starting initial home status update from database via CommandLineRunner...");
            repository.findFirstByOrderByDateDesc()
                .doOnNext(entity -> {
                    log.info("Initial home status found in database: {}", entity.status());
                    homeStatus.setHomeHeatingEnabled(entity.status());
                    homeStatus.setUpdatedAt(entity.date());
                })
                .doOnSuccess(entity -> {
                    if (entity == null) {
                        log.warn("No heating status found in database. Keeping default.");
                    } else {
                        log.info("Initial home status updated successfully.");
                    }
                })
                .doOnError(throwable -> log.error(
                    "Error updating home status from database: {}",
                    throwable.getMessage()
                ))
                .onErrorComplete()
                .subscribe();
        };
    }
}
