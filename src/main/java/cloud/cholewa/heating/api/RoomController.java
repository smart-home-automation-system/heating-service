package cloud.cholewa.heating.api;

import cloud.cholewa.heating.model.RoomReply;
import cloud.cholewa.heating.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/rooms")
    Mono<ResponseEntity<List<RoomReply>>> queryRooms() {
        return roomService.queryRooms()
            .collectList()
            .doOnSubscribe(subscription -> log.info("Querying the state of the rooms"))
            .map(ResponseEntity::ok);
    }

    @GetMapping("/rooms/{name}")
    Mono<ResponseEntity<RoomReply>> queryRoom(@PathVariable final String name) {
        return roomService.queryRoom(name)
            .doOnSubscribe(subscription -> log.info("Querying the state of room: {}", name))
            .map(ResponseEntity::ok);
    }
}
