package cloud.cholewa.heating.api;

import cloud.cholewa.heating.model.ScheduleBody;
import cloud.cholewa.heating.model.StoredSchedule;
import cloud.cholewa.heating.service.HeatingScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

//The writes of the heating schedules (HAS-356); they are read with the rooms (GET /rooms). No @Valid: every
//rule of a body is ScheduleRules', which answers INVALID_SCHEDULE with the reason. A write answers once the
//heaters hold it, so the next pass of the room heats by it
@RestController
@RequiredArgsConstructor
public class HeatingScheduleController {

    private final HeatingScheduleService heatingScheduleService;

    @PostMapping("/rooms/{room}/heaters/{heater}/schedules")
    Mono<ResponseEntity<StoredSchedule>> create(
        @PathVariable final String room,
        @PathVariable final String heater,
        @RequestBody final ScheduleBody body
    ) {
        return heatingScheduleService.create(room, heater, body)
            .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created));
    }

    @PutMapping("/schedules/{id}")
    Mono<StoredSchedule> replace(@PathVariable final long id, @RequestBody final ScheduleBody body) {
        return heatingScheduleService.replace(id, body);
    }

    @PutMapping("/schedules/{id}/enabled")
    Mono<ResponseEntity<Void>> enable(@PathVariable final long id, @RequestParam final boolean value) {
        return heatingScheduleService.enable(id, value)
            .then(Mono.fromSupplier(() -> ResponseEntity.noContent().build()));
    }

    @DeleteMapping("/schedules/{id}")
    Mono<ResponseEntity<Void>> remove(@PathVariable final long id) {
        return heatingScheduleService.remove(id)
            .then(Mono.fromSupplier(() -> ResponseEntity.noContent().build()));
    }
}
