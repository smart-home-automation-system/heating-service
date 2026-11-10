package cloud.cholewa.heating.db.repository;

import cloud.cholewa.heating.db.model.HeatingStatusEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface HeatingStatusRepository extends R2dbcRepository<HeatingStatusEntity, Long> {
    
    Mono<HeatingStatusEntity> findFirstByOrderByDateDesc();

    //The changes of the house switch before "before" (HAS-413), never the latest one - the state of the switch the
    //service loads at the start (findFirstByOrderByDateDesc), however old it is; of two at the same time the later
    //written stays. A few rows a day: no index needed. Run on PostgreSQL by HeatingStatusRepositoryTest
    @Modifying
    @Query("""
        DELETE FROM heating_status
        WHERE date < :before
          AND id <> (SELECT id FROM heating_status ORDER BY date DESC, id DESC LIMIT 1)
        """)
    Mono<Long> deleteBeforeKeepingTheLatest(LocalDateTime before);
}
