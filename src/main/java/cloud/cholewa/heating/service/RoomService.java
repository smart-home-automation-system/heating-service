package cloud.cholewa.heating.service;

import cloud.cholewa.heating.infrastructure.error.RoomNotFoundException;
import cloud.cholewa.heating.mapper.RoomMapper;
import cloud.cholewa.heating.model.Home;
import cloud.cholewa.heating.model.RoomReply;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final Home home;
    private final RoomMapper roomMapper;

    /**
     * Every room of the house, in the order of the configuration. The state is read when the
     * reply is subscribed to, and nothing here asks a device or the database.
     */
    public Flux<RoomReply> queryRooms() {
        return Flux.defer(() -> Flux.fromIterable(home.rooms()))
            .map(roomMapper::toReply);
    }

    /**
     * One room by the name the list gives it ({@code living room}), in any case. A name of
     * {@code RoomName} that the house has no room for is unknown like any other.
     */
    public Mono<RoomReply> queryRoom(final String name) {
        return Mono.defer(() -> Mono.justOrEmpty(
                home.rooms().stream()
                    .filter(room -> room.getName().getValue().equalsIgnoreCase(name))
                    .findFirst()
            ))
            .map(roomMapper::toReply)
            .switchIfEmpty(Mono.error(() -> new RoomNotFoundException(name)));
    }
}
