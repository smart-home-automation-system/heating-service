package cloud.cholewa.heating.config;

import cloud.cholewa.heating.calendar.HouseCalendar;
import cloud.cholewa.heating.service.HeatingScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Loads the heating schedules from the database into the heaters before anything can decide a room
 * (HAS-355). A lifecycle of a low phase, not a {@code CommandLineRunner}: the temperature and device
 * listeners (phase {@code Integer.MAX_VALUE}), the web server and the decision of every minute (registered
 * once the context is refreshed) all start after it - a runner runs after them, and a reading in between
 * would have switched off every heater as out of schedule.
 * The type of today is read from the calendar of the house right after them (HAS-358), never fatal.
 * <p>
 * <b>Fatal when it fails</b>, as the heating switch is (HomeStatusConfig): a service without its schedules
 * would hold every room cold. The rollout then stops at the new pod and the old one keeps heating.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class HeatingScheduleLoader implements SmartLifecycle {

    private static final int PHASE = 0;
    private static final long RETRY_ATTEMPTS = 3;
    private static final Duration RETRY_BACKOFF = Duration.ofMillis(500);
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(30);

    private final HeatingScheduleService service;
    private final HouseCalendar houseCalendar;

    private volatile boolean running;

    @Override
    public void start() {
        service.load()
            .retryWhen(Retry.backoff(RETRY_ATTEMPTS, RETRY_BACKOFF))
            //the throwable, not its message: Retry.backoff replaces it with "Retries exhausted"
            .doOnError(throwable -> log.error("Error loading the heating schedules from the database", throwable))
            //blocking on purpose, bounded: a hung query must not leave the pod starting forever
            .block(STARTUP_TIMEOUT);
        //the type of today too (HAS-358), so a reading in the first minute heats a holiday by its weekend
        //schedule - never fatal: without the calendar the day is its weekday, and refresh never fails
        houseCalendar.refresh().block(STARTUP_TIMEOUT);
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return PHASE;
    }
}
