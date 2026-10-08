package cloud.cholewa.heating.api;

import cloud.cholewa.heating.infrastructure.error.ExceptionHandlerConfig;
import cloud.cholewa.heating.infrastructure.error.RoomNotFoundException;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.heating.model.RoomMode;
import cloud.cholewa.heating.model.RoomReply;
import cloud.cholewa.heating.model.ScheduleType;
import cloud.cholewa.heating.service.RoomService;
import cloud.cholewa.home.model.RoomName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SUNDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

//The JSON is the contract of the web dashboard, so the whole body is written out: a field renamed
//or filled with a default where it used to be left out has to fail here
@ExtendWith(OutputCaptureExtension.class)
@WebFluxTest(RoomController.class)
@Import(ExceptionHandlerConfig.class)
class RoomControllerTest {

    private static final RoomReply LIVING_ROOM = new RoomReply(
        RoomName.LIVING_ROOM,
        RoomMode.HEATING,
        true,
        new RoomReply.Reading(19.4, LocalDateTime.of(2026, 10, 8, 18, 30)),
        new RoomReply.Reading(48, LocalDateTime.of(2026, 10, 8, 18, 29)),
        List.of(
            new RoomReply.Heater(
                HeaterType.RADIATOR,
                true,
                LocalDateTime.of(2026, 10, 8, 18, 31),
                true,
                20.5,
                List.of(new RoomReply.HeaterSchedule(
                    ScheduleType.HEATING, List.of(MONDAY, SUNDAY), LocalTime.of(7, 0), LocalTime.of(23, 0), 20.5
                ))
            ),
            new RoomReply.Heater(HeaterType.FLOOR, null, null, false, null, List.of())
        )
    );

    private static final String LIVING_ROOM_JSON = """
        {
          "name": "living room",
          "mode": "HEATING",
          "heatingEnabled": true,
          "temperature": {"value": 19.4, "updatedAt": "2026-10-08T18:30:00"},
          "humidity": {"value": 48.0, "updatedAt": "2026-10-08T18:29:00"},
          "heaters": [
            {
              "type": "radiator",
              "working": true,
              "updatedAt": "2026-10-08T18:31:00",
              "inSchedule": true,
              "targetTemperature": 20.5,
              "schedules": [
                {"type": "HEATING", "days": ["MONDAY", "SUNDAY"], "startTime": "07:00:00", "endTime": "23:00:00", "temperature": 20.5}
              ]
            },
            {"type": "floor", "inSchedule": false, "schedules": []}
          ]
        }
        """;

    //a room that never reported and has no heater: no temperature, no humidity, an empty list
    private static final RoomReply GARDEN = new RoomReply(RoomName.GARDEN, null, false, null, null, List.of());

    private static final String GARDEN_JSON = """
        {"name": "garden", "heatingEnabled": false, "heaters": []}
        """;

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean(answers = Answers.RETURNS_SMART_NULLS)
    private RoomService roomService;

    @Test
    void should_list_the_rooms() {
        when(roomService.queryRooms()).thenReturn(Flux.just(LIVING_ROOM, GARDEN));

        webTestClient.get()
            .uri("/rooms")
            .exchange()
            .expectStatus().isOk()
            .expectBody().json("[" + LIVING_ROOM_JSON + "," + GARDEN_JSON + "]", JsonCompareMode.STRICT);
    }

    @Test
    void should_return_a_room() {
        when(roomService.queryRoom("living room")).thenReturn(Mono.just(LIVING_ROOM));

        webTestClient.get()
            .uri("/rooms/{name}", "living room")
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(LIVING_ROOM_JSON, JsonCompareMode.STRICT);
    }

    @Test
    void should_return_a_room_without_readings_without_a_temperature() {
        when(roomService.queryRoom("garden")).thenReturn(Mono.just(GARDEN));

        webTestClient.get()
            .uri("/rooms/garden")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .json(GARDEN_JSON, JsonCompareMode.STRICT)
            .jsonPath("$.temperature").doesNotExist();
    }

    //the code is what tells "no such room" from a 404 of the routing, which carries none
    @Test
    void should_answer_404_with_a_code_for_an_unknown_room(final CapturedOutput output) {
        when(roomService.queryRoom("attic")).thenReturn(Mono.error(new RoomNotFoundException("attic")));

        webTestClient.get()
            .uri("/rooms/attic")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.errors.length()").isEqualTo(1)
            .jsonPath("$.errors[0].code").isEqualTo("NOT_FOUND_ROOM")
            .jsonPath("$.errors[0].message").isEqualTo("Room with provided name is not a part of home")
            .jsonPath("$.errors[0].details").isEqualTo("Room name: attic");

        //the level is what the alerts see: a mistyped room polled by a dashboard must not read as
        //an error of the service, and must not pass without a trace either
        assertThat(output.getOut().lines().filter(line -> line.contains("Handled [RoomNotFoundException]")))
            .singleElement()
            .satisfies(line -> assertThat(line).contains(" WARN ").contains("attic"));
    }

    @Test
    void should_answer_404_without_a_code_for_a_path_that_is_not_served() {
        webTestClient.get()
            .uri("/rooms/attic/heaters")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.errors.length()").isEqualTo(1)
            .jsonPath("$.errors[0].message").exists()
            .jsonPath("$.errors[0].code").doesNotExist();
    }
}
