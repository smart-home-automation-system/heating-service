package cloud.cholewa.heating.config;

import cloud.cholewa.heating.infrastructure.error.HeatingException;
import cloud.cholewa.home.model.RoomName;
import cloud.cholewa.heating.model.HeaterType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RelayConfigTest {

    private RelayConfig sut;

    @BeforeEach
    void setUp() {
        sut = new RelayConfig();
        ReflectionTestUtils.setField(sut, "heater", Map.of("bedroom", 1, "living_room", 2));
        ReflectionTestUtils.setField(sut, "floor", Map.of("bathroom_up", 3, "kitchen", 4));
    }

    @Test
    void should_keep_the_schedules_of_a_radiator_left_to_another_service() {
        sut.setScheduleOnly(List.of("sanctum"));

        assertThat(sut.keepsSchedules(RoomName.SANCTUM, HeaterType.RADIATOR)).isTrue();
        assertThat(sut.drives(RoomName.SANCTUM, HeaterType.RADIATOR)).isFalse();
        assertThat(sut.keepsSchedules(RoomName.SANCTUM, HeaterType.FLOOR)).isFalse();
        assertThat(sut.keepsSchedules(RoomName.BEDROOM, HeaterType.RADIATOR)).isTrue();
    }

    @Test
    void should_drive_a_relay_only_for_a_relay_of_a_pro_4() {
        sut.setStandalone(Map.of("sanctum", "192.0.2.135"));

        assertThat(sut.drivesRelay(RoomName.BEDROOM, HeaterType.RADIATOR)).isTrue();
        assertThat(sut.drivesRelay(RoomName.SANCTUM, HeaterType.RADIATOR)).isFalse();
        assertThat(sut.drives(RoomName.SANCTUM, HeaterType.RADIATOR)).isTrue();
    }

    @Test
    void should_get_heater_relay_number() {
        assertThat(sut.getHeater(RoomName.BEDROOM)).isEqualTo(1);
        assertThat(sut.getHeater(RoomName.LIVING_ROOM)).isEqualTo(2);
    }

    @Test
    void should_throw_exception_when_heater_room_not_found() {
        assertThatThrownBy(() -> sut.getHeater(RoomName.GARAGE))
            .isInstanceOf(HeatingException.class)
            .hasMessageContaining("Unknown configuration for room heater: garage");
    }

    @Test
    void should_get_floor_relay_number() {
        assertThat(sut.getFloor(RoomName.BATHROOM_UP)).isEqualTo(3);
        assertThat(sut.getFloor(RoomName.KITCHEN)).isEqualTo(4);
    }

    @Test
    void should_throw_exception_when_floor_room_not_found() {
        assertThatThrownBy(() -> sut.getFloor(RoomName.GARAGE))
            .isInstanceOf(HeatingException.class)
            .hasMessageContaining("Unknown configuration for room floor: garage");
    }
}