package cloud.cholewa.heating.mapper;

import cloud.cholewa.heating.db.model.TemperatureEntity;
import cloud.cholewa.home.model.TemperatureMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TemperatureMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "room", source = "room.value")
    TemperatureEntity toEntity(TemperatureMessage message);

}
