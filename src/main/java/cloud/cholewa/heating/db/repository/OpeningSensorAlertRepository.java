package cloud.cholewa.heating.db.repository;

import cloud.cholewa.heating.db.model.OpeningSensorAlertEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface OpeningSensorAlertRepository extends R2dbcRepository<OpeningSensorAlertEntity, Long> {
}
