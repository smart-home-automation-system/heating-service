package cloud.cholewa.heating.db.repository;

import cloud.cholewa.heating.db.model.TemperatureSensorAlertEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface TemperatureSensorAlertRepository extends R2dbcRepository<TemperatureSensorAlertEntity, Long> {

    Mono<TemperatureSensorAlertEntity> findByRoom(String room);
}
