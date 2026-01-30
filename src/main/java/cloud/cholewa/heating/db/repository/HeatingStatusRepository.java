package cloud.cholewa.heating.db.repository;

import cloud.cholewa.heating.db.model.HeatingStatusEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface HeatingStatusRepository extends R2dbcRepository<HeatingStatusEntity, Long> {
    
    Mono<HeatingStatusEntity> findFirstByOrderByDateDesc();
}
