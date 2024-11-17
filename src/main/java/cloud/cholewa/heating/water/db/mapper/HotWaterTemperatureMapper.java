package cloud.cholewa.heating.water.db.mapper;

import cloud.cholewa.heating.water.db.model.HotWaterTemperatureEntity;
import cloud.cholewa.shelly.model.ShellyUniStatusResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class HotWaterTemperatureMapper {

    public static HotWaterTemperatureEntity toEntity(final ShellyUniStatusResponse response) {
        return HotWaterTemperatureEntity.builder()
            .timestamp(getLocalDateTime(Objects.requireNonNull(response.getUnixtime())))
            .waterTemperature(response.getExtTemperature().get("0").gettC())
            .circulationTemperature(response.getExtTemperature().get("1").gettC())
            .build();
    }

    private static LocalDateTime getLocalDateTime(final int unixTime) {
        //log.info("LocalDateTime: {}", localDateTime);
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(unixTime), ZoneId.systemDefault());
    }
}
