package cloud.cholewa.heating.window;

import java.time.LocalDateTime;

/**
 * The windows and doors of one room at a moment (HAS-232).
 *
 * @param state     what the sensors of the room say together
 * @param openSince since when the window that has stood open longest is open - by the clock of the
 *                  gateway that heard it, never later than when this service learnt it; only while
 *                  {@code state} is open
 * @param hold      whether the heating of the room is held: a window or a door has stood open for
 *                  {@code heating.window.hold-after} at least. Never for a sensor that is not known -
 *                  a silent sensor must not leave a room cold
 */
public record RoomWindow(WindowState state, LocalDateTime openSince, boolean hold) {

    public static final RoomWindow NO_SENSOR = new RoomWindow(WindowState.NO_SENSOR, null, false);
}
