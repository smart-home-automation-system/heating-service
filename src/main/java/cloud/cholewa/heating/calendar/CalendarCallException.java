package cloud.cholewa.heating.calendar;

//a read of the calendar of the house that did not end with the type of the day asked for; never
//answered - the day is then judged by its weekday, with a WARN (HouseCalendar)
public class CalendarCallException extends RuntimeException {

    public CalendarCallException(final String message) {
        super(message);
    }
}
