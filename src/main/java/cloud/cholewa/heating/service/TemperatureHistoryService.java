package cloud.cholewa.heating.service;

import cloud.cholewa.heating.db.repository.TemperatureRepository;
import cloud.cholewa.heating.model.TemperatureHistoryReply;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TemperatureHistoryService {

    private final RoomService roomService;
    private final TemperatureRepository temperatureRepository;

    /**
     * The stored temperatures of a room from {@code from} up to, not including, {@code to}, as
     * averages of buckets whose width follows from the length of the range. The room is named as
     * the list of rooms names it, in any case; the readings are asked for by the name the room has
     * in the configuration, which is how they are stored - never by the text of the request.
     * A room of the house without a reading in the range is answered with no points.
     */
    public Mono<TemperatureHistoryReply> queryHistory(
        final String name,
        final LocalDateTime from,
        final LocalDateTime to
    ) {
        return Mono.defer(() -> HistoryRange.violation(from, to)
            .<Mono<TemperatureHistoryReply>>map(Mono::error)
            .orElseGet(() -> roomService.queryRoomName(name)
                .flatMap(room -> {
                    final long bucketSeconds = HistoryRange.bucket(from, to).toSeconds();

                    return temperatureRepository.findHistory(room.getValue(), from, to, bucketSeconds)
                        .map(bucket -> new TemperatureHistoryReply.Point(bucket.at(), bucket.temperature()))
                        .collectList()
                        .map(points -> new TemperatureHistoryReply(room, from, to, bucketSeconds, points));
                })));
    }
}
