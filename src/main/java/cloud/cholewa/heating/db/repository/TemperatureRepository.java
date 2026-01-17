package cloud.cholewa.heating.db.repository;

import cloud.cholewa.heating.db.model.TemperatureEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface TemperatureRepository extends R2dbcRepository<TemperatureEntity, Long> {
}
