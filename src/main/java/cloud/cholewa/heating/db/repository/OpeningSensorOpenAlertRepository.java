package cloud.cholewa.heating.db.repository;

import cloud.cholewa.heating.db.model.OpeningSensorOpenAlertEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface OpeningSensorOpenAlertRepository extends R2dbcRepository<OpeningSensorOpenAlertEntity, Long> {
}
