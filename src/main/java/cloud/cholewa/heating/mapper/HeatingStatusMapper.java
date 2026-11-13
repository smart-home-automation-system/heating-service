package cloud.cholewa.heating.mapper;

import cloud.cholewa.heating.db.model.HeatingStatusEntity;
import cloud.cholewa.heating.model.HeatingStatusReply;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Mapper(componentModel = "spring", imports = {LocalDateTime.class, ZoneId.class})
public interface HeatingStatusMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "date", expression = "java(LocalDateTime.now(ZoneId.systemDefault()))")
    @Mapping(target = "status", source = "isHeatingEnabled")
    HeatingStatusEntity toEntity(Boolean isHeatingEnabled);
    
    @Mapping(target = "updatedAt", source = "entity.date")
    @Mapping(target = "isHeatingEnabled", source = "entity.status")
    HeatingStatusReply toReply(HeatingStatusEntity entity);
}
