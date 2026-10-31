package cloud.cholewa.heating.calendar;

import org.mockito.Mockito;

import java.time.Clock;

//A calendar of the house never asked: every date is its weekday - what a test of something else needs
public final class HouseCalendarFixture {

    private HouseCalendarFixture() {
    }

    public static HouseCalendar weekdays() {
        return new HouseCalendar(Mockito.mock(CalendarClient.class), Clock.systemDefaultZone());
    }
}
