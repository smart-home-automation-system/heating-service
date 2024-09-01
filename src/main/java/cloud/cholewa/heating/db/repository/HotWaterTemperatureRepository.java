package cloud.cholewa.heating.db.repository;

import cloud.cholewa.heating.db.model.HotWaterTemperatureEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HotWaterTemperatureRepository extends R2dbcRepository<HotWaterTemperatureEntity, Long> {
}
