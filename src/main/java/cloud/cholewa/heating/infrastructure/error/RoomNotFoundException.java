package cloud.cholewa.heating.infrastructure.error;

//the message is the details of the 404 as the dashboard has always had it: "Room name: <name>" (HAS-215 -
//the prefix was the processor's until the processor came from cholewa-commons)
public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException(final String name) {
        super("Room name: " + name);
    }
}
