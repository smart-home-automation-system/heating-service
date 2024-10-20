package cloud.cholewa.heating.home;

import cloud.cholewa.heating.home.model.RoomConfigurationResponse;
import cloud.cholewa.heating.model.HeaterActor;
import cloud.cholewa.heating.model.OpeningSensor;
import cloud.cholewa.heating.model.Room;
import cloud.cholewa.heating.model.RoomNames;
import cloud.cholewa.heating.model.TemperatureSensor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RoomMapperTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("roomConfig")
    void should_map_room_to_roomConfigurationResponse(
        final String name,
        final Room room,
        final RoomConfigurationResponse roomConfigurationResponse
    ) {
        assertThat(RoomMapper.map(room)).isEqualTo(roomConfigurationResponse);
    }

    private static Stream<Arguments> roomConfig() {
        return Stream.of(
            Arguments.of(
                "room with temp sensor, opening sensor and radiator",
                Room.builder()
                    .name(RoomNames.MAIN)
                    .temperatureSensor(TemperatureSensor.builder().build())
                    .openingSensors(List.of(OpeningSensor.builder().build()))
                    .heaterActors(List.of(HeaterActor.builder().build()))
                    .build(),
                RoomConfigurationResponse.builder()
                    .name("MAIN")
                    .temperature("unknown")
                    .isAnyOpeningOpened(false)
                    .isHeatingActive(false)
                    .build()
            ),
            Arguments.of(
                "room with temp sensor, w/o opening sensor and with radiator",
                Room.builder()
                    .name(RoomNames.MAIN)
                    .temperatureSensor(TemperatureSensor.builder().build())
                    .heaterActors(List.of(HeaterActor.builder().build()))
                    .build(),
                RoomConfigurationResponse.builder()
                    .name("MAIN")
                    .temperature("unknown")
                    .isAnyOpeningOpened(false)
                    .isHeatingActive(false)
                    .build()
            ),
            Arguments.of(
                "room with temp sensor, opening sensors (one opened) and with radiator",
                Room.builder()
                    .name(RoomNames.MAIN)
                    .temperatureSensor(TemperatureSensor.builder().build())
                    .openingSensors(List.of(
                        OpeningSensor.builder().isOpen(true).build(),
                        OpeningSensor.builder().isOpen(false).build()
                        )
                    )
                    .heaterActors(List.of(HeaterActor.builder().build()))
                    .build(),
                RoomConfigurationResponse.builder()
                    .name("MAIN")
                    .temperature("unknown")
                    .isAnyOpeningOpened(true)
                    .isHeatingActive(false)
                    .build()
            ),
            Arguments.of(
                "room with temp sensor, opening sensors (both opened) and with radiator",
                Room.builder()
                    .name(RoomNames.MAIN)
                    .temperatureSensor(TemperatureSensor.builder().build())
                    .openingSensors(List.of(
                            OpeningSensor.builder().isOpen(true).build(),
                            OpeningSensor.builder().isOpen(true).build()
                        )
                    )
                    .heaterActors(List.of(HeaterActor.builder().build()))
                    .build(),
                RoomConfigurationResponse.builder()
                    .name("MAIN")
                    .temperature("unknown")
                    .isAnyOpeningOpened(true)
                    .isHeatingActive(false)
                    .build()
            ),
            Arguments.of(
                "room with temp sensor with value, opening sensors (one opened) and with radiator",
                Room.builder()
                    .name(RoomNames.OFFICE)
                    .temperatureSensor(TemperatureSensor.builder()
                        .updateTime(LocalDateTime.now())
                        .temperature(20.3).build())
                    .openingSensors(List.of(
                            OpeningSensor.builder().isOpen(true).build(),
                            OpeningSensor.builder().isOpen(false).build()
                        )
                    )
                    .heaterActors(List.of(HeaterActor.builder().build()))
                    .build(),
                RoomConfigurationResponse.builder()
                    .name("OFFICE")
                    .temperature("20.3")
                    .isAnyOpeningOpened(true)
                    .isHeatingActive(false)
                    .build()
            )
        );
    }
}