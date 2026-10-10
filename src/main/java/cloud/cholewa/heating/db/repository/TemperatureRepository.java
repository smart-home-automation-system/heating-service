package cloud.cholewa.heating.db.repository;

import cloud.cholewa.heating.db.model.TemperatureBucket;
import cloud.cholewa.heating.db.model.TemperatureEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface TemperatureRepository extends R2dbcRepository<TemperatureEntity, Long> {

    Mono<TemperatureEntity> findFirstByRoomOrderByDateDesc(String room);

    //The readings of a room from "from" up to, not including, "to", averaged into buckets of
    //"bucketSeconds", oldest first; a bucket without a reading has no row. The column holds the
    //wall-clock time of the house without a zone, and the epoch of such a value counts from its
    //own midnight - so a bucket that divides a day starts on the clock of the house (00:00,
    //00:20, ...), whatever "from" is: a range that starts inside a bucket gets a first row whose
    //"at" is before it, averaged from the readings within the range. On that clock the hour
    //repeated when the summer time ends falls into the same buckets twice, and the hour skipped
    //when it begins has none. The database does the averaging: a month of the busiest room is some
    //15 000 rows, read through the index on (room, date) and answered as 248 (170 ms on the
    //production database, 2026-10-09).
    //PostgreSQL only: TemperatureRepositoryTest runs it against a PostgreSQL in Docker.
    @Query("""
        SELECT TIMESTAMP 'epoch'
                   + floor(extract(epoch FROM date) / :bucketSeconds) * :bucketSeconds * INTERVAL '1 second' AS at,
               round(avg(temperature), 2) AS temperature
        FROM room_temperature
        WHERE room = :room
          AND date >= :from
          AND date < :to
        GROUP BY at
        ORDER BY at
        """)
    Flux<TemperatureBucket> findHistory(String room, LocalDateTime from, LocalDateTime to, long bucketSeconds);
}
