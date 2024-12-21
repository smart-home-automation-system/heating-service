package cloud.cholewa.heating.home.config;

import cloud.cholewa.heating.model.BoilerRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;

@Slf4j
@Component
@RequiredArgsConstructor
public class HomeHeatingEvent {

    private final BoilerRoom boilerRoom;

    @EventListener(ApplicationReadyEvent.class)
    public void handleEnablingHomeHeating() {
        log.info("Configuring home heating ...");
        boilerRoom.setHeatingEnabled(checkMonthOfYear());
        log.info("Home heating enabled: [{}]", boilerRoom.isHeatingEnabled());
    }

    private boolean checkMonthOfYear() {
        return LocalDate.now().getMonth().getValue() >= Month.OCTOBER.getValue()
            || LocalDate.now().getMonth().getValue() <= Month.MARCH.getValue();
    }
}
