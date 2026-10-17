package cloud.cholewa.heating.window;

import org.mockito.quality.Strictness;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

//window sensors for the tests that need some and are not about them: every room has none
public final class WindowSensorsFixture {

    private WindowSensorsFixture() {
    }

    public static WindowSensors none() {
        //lenient: a test that never asks for a window must not fail for it under strict stubs
        final WindowSensors windowSensors = mock(WindowSensors.class, withSettings().strictness(Strictness.LENIENT));
        when(windowSensors.windowOf(any())).thenReturn(RoomWindow.NO_SENSOR);
        when(windowSensors.windowOf(any(), any())).thenReturn(RoomWindow.NO_SENSOR);
        return windowSensors;
    }
}
