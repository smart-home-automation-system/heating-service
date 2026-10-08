package cloud.cholewa.heating.config;

import cloud.cholewa.heating.service.RoomTemperatureSeeder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomTemperatureConfigTest {

    @Mock
    private RoomTemperatureSeeder seeder;

    private final RoomTemperatureConfig sut = new RoomTemperatureConfig();

    @Test
    void should_seed_the_rooms_at_startup() throws Exception {
        final AtomicBoolean seeded = new AtomicBoolean();
        when(seeder.seed()).thenReturn(Mono.fromRunnable(() -> seeded.set(true)));

        sut.initRoomTemperatures(seeder).run();

        assertThat(seeded).isTrue();
    }

    //blank rooms are no reason to keep the heating of the house from starting
    @Test
    void should_not_fail_the_startup_when_the_rooms_cannot_be_seeded() {
        when(seeder.seed()).thenReturn(Mono.error(new IllegalStateException("database unavailable")));

        assertThatCode(() -> sut.initRoomTemperatures(seeder).run()).doesNotThrowAnyException();
    }
}
