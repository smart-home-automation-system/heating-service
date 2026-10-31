package cloud.cholewa.heating.api;

import cloud.cholewa.heating.infrastructure.error.ExceptionHandlerConfig;
import cloud.cholewa.heating.infrastructure.error.RoomNotFoundException;
import cloud.cholewa.heating.infrastructure.error.ScheduleException;
import cloud.cholewa.heating.model.HeaterType;
import cloud.cholewa.heating.model.ScheduleBody;
import cloud.cholewa.heating.model.StoredSchedule;
import cloud.cholewa.heating.service.HeatingScheduleService;
import cloud.cholewa.home.model.RoomName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//The writes of the heating schedules (HAS-356) as a caller sees them: the paths, the bodies, the answers and
//the codes the dashboard words
@ExtendWith(OutputCaptureExtension.class)
@WebFluxTest(HeatingScheduleController.class)
@Import(ExceptionHandlerConfig.class)
class HeatingScheduleControllerTest {

    private static final String BODY = """
        {"days": ["SATURDAY", "SUNDAY"], "startTime": "09:00", "endTime": "22:00", "temperature": 21.0}
        """;
    private static final ScheduleBody READ = new ScheduleBody(List.of("SATURDAY", "SUNDAY"), "09:00", "22:00", new BigDecimal("21.0"));
    private static final StoredSchedule STORED = new StoredSchedule(
        31, RoomName.LIVING_ROOM, HeaterType.RADIATOR, List.of(SATURDAY, SUNDAY), LocalTime.of(9, 0), LocalTime.of(22, 0), 21.0, true);
    //the times written as GET /rooms writes those of a schedule
    private static final String STORED_JSON = """
        {"id": 31, "room": "living room", "heater": "radiator", "days": ["SATURDAY", "SUNDAY"],
         "startTime": "09:00:00", "endTime": "22:00:00", "temperature": 21.0, "enabled": true}
        """;

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean(answers = Answers.RETURNS_SMART_NULLS)
    private HeatingScheduleService heatingScheduleService;

    @Test
    void should_create_a_schedule_of_a_heater() {
        when(heatingScheduleService.create("living room", "radiator", READ)).thenReturn(Mono.just(STORED));

        webTestClient.post()
            .uri("/rooms/{room}/heaters/{heater}/schedules", "living room", "radiator")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(BODY)
            .exchange()
            .expectStatus().isCreated()
            .expectBody().json(STORED_JSON, JsonCompareMode.STRICT);
    }

    @Test
    void should_replace_a_schedule() {
        when(heatingScheduleService.replace(31L, READ)).thenReturn(Mono.just(STORED));

        webTestClient.put()
            .uri("/schedules/31")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(BODY)
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(STORED_JSON, JsonCompareMode.STRICT);
    }

    @Test
    void should_switch_a_schedule_off() {
        when(heatingScheduleService.enable(31L, false)).thenReturn(Mono.empty());

        webTestClient.put()
            .uri("/schedules/31/enabled?value=false")
            .exchange()
            .expectStatus().isNoContent()
            .expectBody().isEmpty();

        verify(heatingScheduleService).enable(31L, false);
    }

    @Test
    void should_refuse_a_switch_without_its_value() {
        webTestClient.put()
            .uri("/schedules/31/enabled")
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void should_remove_a_schedule() {
        when(heatingScheduleService.remove(31L)).thenReturn(Mono.empty());

        webTestClient.delete()
            .uri("/schedules/31")
            .exchange()
            .expectStatus().isNoContent()
            .expectBody().isEmpty();

        verify(heatingScheduleService).remove(31L);
    }

    //the reason is answered, and logged at WARN: a refused write is the caller's, not an error of the service
    @Test
    void should_answer_400_invalid_schedule_with_the_reason(final CapturedOutput output) {
        when(heatingScheduleService.replace(31L, READ))
            .thenReturn(Mono.error(ScheduleException.invalid("the window overlaps schedule 5 (SATURDAY 17:00-22:00)")));

        webTestClient.put()
            .uri("/schedules/31")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(BODY)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.errors.length()").isEqualTo(1)
            .jsonPath("$.errors[0].code").isEqualTo("INVALID_SCHEDULE")
            .jsonPath("$.errors[0].message").isEqualTo("Invalid schedule")
            .jsonPath("$.errors[0].details").isEqualTo("the window overlaps schedule 5 (SATURDAY 17:00-22:00)");

        assertThat(output.getOut().lines().filter(line -> line.contains("Heating schedule refused")))
            .singleElement()
            .satisfies(line -> assertThat(line).contains(" WARN ").contains("INVALID_SCHEDULE"));
    }

    @Test
    void should_answer_404_not_found_schedule() {
        when(heatingScheduleService.remove(9L)).thenReturn(Mono.error(ScheduleException.notFound(9L)));

        webTestClient.delete()
            .uri("/schedules/9")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.errors[0].code").isEqualTo("NOT_FOUND_SCHEDULE")
            .jsonPath("$.errors[0].details").isEqualTo("no schedule with id 9");
    }

    @Test
    void should_answer_404_not_found_heater() {
        when(heatingScheduleService.create("sanctum", "radiator", READ))
            .thenReturn(Mono.error(ScheduleException.heaterNotFound("sanctum", "radiator")));

        webTestClient.post()
            .uri("/rooms/sanctum/heaters/radiator/schedules")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(BODY)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.errors[0].code").isEqualTo("NOT_FOUND_HEATER")
            .jsonPath("$.errors[0].details").isEqualTo("room sanctum has no radiator this service drives");
    }

    @Test
    void should_answer_404_not_found_room() {
        when(heatingScheduleService.create("attic", "radiator", READ)).thenReturn(Mono.error(new RoomNotFoundException("attic")));

        webTestClient.post()
            .uri("/rooms/attic/heaters/radiator/schedules")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(BODY)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.errors[0].code").isEqualTo("NOT_FOUND_ROOM");
    }
}
