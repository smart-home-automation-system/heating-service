package cloud.cholewa.heating.config;

import cloud.cholewa.heating.service.RoomTemperatureSeeder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Configuration
public class RoomTemperatureConfig {

    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(30);

    @Bean
    @Profile("!test")
    CommandLineRunner initRoomTemperatures(final RoomTemperatureSeeder seeder) {
        //blocking, so that the pod turns Ready with the rooms already filled - but, unlike the
        //heating switch in HomeStatusConfig, never fatal: without it the rooms are merely blank
        //until their sensors report, and the heating works all the same
        return args -> seeder.seed()
            .timeout(STARTUP_TIMEOUT)
            .onErrorResume(throwable -> {
                log.error("Error while reading the last stored temperatures of the rooms", throwable);
                return Mono.empty();
            })
            .block();
    }
}
