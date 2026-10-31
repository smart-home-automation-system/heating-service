package cloud.cholewa.heating.model;

//what the temperature of a schedule is a limit of; the house is only heated (COOLING was never used and
//went with HAS-355 - the stored schedules have no type)
public enum ScheduleType {
    HEATING,
}
