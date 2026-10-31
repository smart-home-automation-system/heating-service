package cloud.cholewa.heating.db.repository;

import cloud.cholewa.heating.db.model.HeatingScheduleEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface HeatingScheduleRepository extends R2dbcRepository<HeatingScheduleEntity, Long> {

    //by id: the order of the schedules of a heater decides which one the control loop heats to
    Flux<HeatingScheduleEntity> findAllByOrderByIdAsc();

    //the schedules of one heater, for the overlap of a written one (HAS-356)
    Flux<HeatingScheduleEntity> findAllByRoomAndHeater(String room, String heater);
}
