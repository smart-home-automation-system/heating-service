package cloud.cholewa.heating.db.repository;

import cloud.cholewa.heating.db.model.WindowSensorReadingEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;

public interface WindowSensorReadingRepository extends R2dbcRepository<WindowSensorReadingEntity, Long> {

    //one statement, PostgreSQL's own: a row per sensor, replaced unless the row holds a reading its gateway heard
    //later - the MQTT listener and the read of the gateways write side by side, and an older one must not win
    @Modifying
    @Query("""
        INSERT INTO window_sensor_reading (sensor_id, open, updated_at, open_since, stored_at)
        VALUES (:sensorId, :open, :updatedAt, :openSince, :storedAt)
        ON CONFLICT (sensor_id) DO UPDATE
        SET open = EXCLUDED.open,
            updated_at = EXCLUDED.updated_at,
            open_since = EXCLUDED.open_since,
            stored_at = EXCLUDED.stored_at
        WHERE window_sensor_reading.updated_at IS NULL
           OR EXCLUDED.updated_at IS NULL
           OR EXCLUDED.updated_at >= window_sensor_reading.updated_at
        """)
    Mono<Integer> upsert(Long sensorId, Boolean open, Instant updatedAt, LocalDateTime openSince, LocalDateTime storedAt);

    //the rows of the sensors the registry no longer lists; never called with an empty collection
    @Modifying
    @Query("DELETE FROM window_sensor_reading WHERE sensor_id NOT IN (:sensorIds)")
    Mono<Integer> deleteOthers(Collection<Long> sensorIds);
}
