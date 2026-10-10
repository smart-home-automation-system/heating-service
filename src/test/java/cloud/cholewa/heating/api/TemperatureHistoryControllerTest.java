package cloud.cholewa.heating.api;

import cloud.cholewa.heating.infrastructure.error.ExceptionHandlerConfig;
import cloud.cholewa.heating.infrastructure.error.RoomNotFoundException;
import cloud.cholewa.heating.model.TemperatureHistoryReply;
import cloud.cholewa.heating.service.TemperatureHistoryService;
import cloud.cholewa.home.model.RoomName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

//The JSON is the contract of the charts of the web dashboard (HAS-201), so the whole body is
//written out
@WebFluxTest(TemperatureHistoryController.class)
@Import(ExceptionHandlerConfig.class)
class TemperatureHistoryControllerTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 10, 8, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 10, 9, 0, 0);
    private static final String HISTORY = "/rooms/{name}/temperature/history?from={from}&to={to}";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean(answers = Answers.RETURNS_SMART_NULLS)
    private TemperatureHistoryService temperatureHistoryService;

    @Test
    void should_return_the_history_of_a_room() {
        when(temperatureHistoryService.queryHistory("living room", FROM, TO)).thenReturn(Mono.just(
            new TemperatureHistoryReply(
                RoomName.LIVING_ROOM, FROM, TO, 1200, List.of(
                new TemperatureHistoryReply.Point(LocalDateTime.of(2026, 10, 8, 0, 0), 21.44),
                new TemperatureHistoryReply.Point(LocalDateTime.of(2026, 10, 8, 0, 20), 21.38)
            ))
        ));

        webTestClient.get()
            .uri(HISTORY, "living room", "2026-10-08T00:00:00", "2026-10-09T00:00:00")
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(
                """
                    {
                      "room": "living room",
                      "from": "2026-10-08T00:00:00",
                      "to": "2026-10-09T00:00:00",
                      "bucketSeconds": 1200,
                      "points": [
                        {"at": "2026-10-08T00:00:00", "value": 21.44},
                        {"at": "2026-10-08T00:20:00", "value": 21.38}
                      ]
                    }
                    """, JsonCompareMode.STRICT
            );
    }

    //no readings in the range is an answer, not a failure: an empty list, never a missing one
    @Test
    void should_return_an_empty_history_as_an_empty_list() {
        when(temperatureHistoryService.queryHistory("garden", FROM, TO)).thenReturn(Mono.just(
            new TemperatureHistoryReply(RoomName.GARDEN, FROM, TO, 1200, List.of())
        ));

        webTestClient.get()
            .uri(HISTORY, "garden", "2026-10-08T00:00:00", "2026-10-09T00:00:00")
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(
                """
                    {
                      "room": "garden",
                      "from": "2026-10-08T00:00:00",
                      "to": "2026-10-09T00:00:00",
                      "bucketSeconds": 1200,
                      "points": []
                    }
                    """, JsonCompareMode.STRICT
            );
    }

    //read with the offset dropped, 22:00Z would be answered as 22:00 of the house
    @ParameterizedTest
    @ValueSource(strings = {"2026-10-08T00:00:00Z", "2026-10-08T00:00:00+02:00", "2026-10-08", "yesterday", ""})
    void should_answer_400_for_a_bound_that_is_not_a_local_date_time(final String from) {
        webTestClient.get()
            .uri(HISTORY, "living room", from, "2026-10-09T00:00:00")
            .exchange()
            .expectStatus().isBadRequest();

        verifyNoInteractions(temperatureHistoryService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/rooms/living room/temperature/history",
        "/rooms/living room/temperature/history?from=2026-10-08T00:00:00",
        "/rooms/living room/temperature/history?to=2026-10-09T00:00:00"
    })
    void should_answer_400_without_both_bounds(final String uri) {
        webTestClient.get()
            .uri(uri)
            .exchange()
            .expectStatus().isBadRequest();

        verifyNoInteractions(temperatureHistoryService);
    }

    @Test
    void should_answer_400_with_the_reason_for_a_range_the_service_refuses() {
        when(temperatureHistoryService.queryHistory("living room", TO, FROM)).thenReturn(Mono.error(
            new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before to")
        ));

        webTestClient.get()
            .uri(HISTORY, "living room", "2026-10-09T00:00:00", "2026-10-08T00:00:00")
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.errors.length()").isEqualTo(1)
            .jsonPath("$.errors[0].message").value(String.class, message ->
                org.assertj.core.api.Assertions.assertThat(message).contains("from must be before to"));
    }

    //the code is what tells "no such room" from a 404 of the routing, as for the room itself
    @Test
    void should_answer_404_with_a_code_for_an_unknown_room() {
        when(temperatureHistoryService.queryHistory("attic", FROM, TO))
            .thenReturn(Mono.error(new RoomNotFoundException("attic")));

        webTestClient.get()
            .uri(HISTORY, "attic", "2026-10-08T00:00:00", "2026-10-09T00:00:00")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.errors.length()").isEqualTo(1)
            .jsonPath("$.errors[0].code").isEqualTo("NOT_FOUND_ROOM")
            .jsonPath("$.errors[0].details").isEqualTo("Room name: attic");
    }
}
