package cloud.cholewa.heating.config;

import cloud.cholewa.heating.db.model.HeatingStatusEntity;
import cloud.cholewa.heating.db.repository.HeatingStatusRepository;
import cloud.cholewa.heating.model.HomeStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeStatusConfigTest {

    @Mock
    private HeatingStatusRepository repository;

    @InjectMocks
    private HomeStatusConfig sut;

    @Test
    void should_initialize_home_status_from_database() throws Exception {
        HomeStatus homeStatus = new HomeStatus();
        LocalDateTime date = LocalDateTime.now();
        HeatingStatusEntity entity = new HeatingStatusEntity(1L, date, true);

        when(repository.findFirstByOrderByDateDesc()).thenReturn(Mono.just(entity));

        CommandLineRunner runner = sut.initHomeStatus(homeStatus, repository);
        runner.run();

        assertThat(homeStatus.isEnabledHomeHeatingSystem()).isTrue();
        assertThat(homeStatus.getHomeHeatingSystemUpdatedAt()).isEqualTo(date);
        verify(repository).findFirstByOrderByDateDesc();
    }

    @Test
    void should_handle_empty_database_on_initialization() throws Exception {
        HomeStatus homeStatus = new HomeStatus();
        homeStatus.setEnabledHomeHeatingSystem(false);

        when(repository.findFirstByOrderByDateDesc()).thenReturn(Mono.empty());

        CommandLineRunner runner = sut.initHomeStatus(homeStatus, repository);
        runner.run();

        assertThat(homeStatus.isEnabledHomeHeatingSystem()).isFalse();
        verify(repository).findFirstByOrderByDateDesc();
    }

    @Test
    void should_fail_startup_when_database_is_unavailable() {
        HomeStatus homeStatus = new HomeStatus();

        AtomicInteger subscriptions = new AtomicInteger();
        when(repository.findFirstByOrderByDateDesc()).thenReturn(Mono.defer(() -> {
            subscriptions.incrementAndGet();
            return Mono.error(new RuntimeException("DB Error"));
        }));

        CommandLineRunner runner = sut.initHomeStatus(homeStatus, repository);

        assertThatThrownBy(runner::run).hasRootCauseMessage("DB Error");

        assertThat(homeStatus.getHomeHeatingSystemUpdatedAt()).isNull();
        assertThat(subscriptions).hasValue(4);
        verify(repository).findFirstByOrderByDateDesc();
    }
}
