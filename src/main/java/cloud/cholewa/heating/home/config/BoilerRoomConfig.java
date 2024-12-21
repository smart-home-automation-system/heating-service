package cloud.cholewa.heating.home.config;

import cloud.cholewa.heating.model.Alert;
import cloud.cholewa.heating.model.AlertReason;
import cloud.cholewa.heating.model.BoilerRoom;
import cloud.cholewa.heating.model.Fireplace;
import cloud.cholewa.heating.model.Furnace;
import cloud.cholewa.heating.model.HotWater;
import cloud.cholewa.heating.model.Pump;
import cloud.cholewa.heating.model.PumpType;
import cloud.cholewa.heating.model.Temperature;
import cloud.cholewa.heating.model.WaterCirculation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class BoilerRoomConfig {

    @Bean
    Fireplace fireplace() {
        return new Fireplace(new Temperature());
    }

    @Bean
    Alert alert() {
        return Alert.builder().reason(AlertReason.NO_ALERT).build();
    }

    @Bean
    BoilerRoom boilerRoom(
        final Alert alert,
        final Furnace furnace,
        final Fireplace fireplace,
        final HotWater hotWater,
        final List<Pump> pumps
    ) {
        return new BoilerRoom(alert, furnace, fireplace, hotWater, pumps);
    }

    @Bean
    HotWater hotWater() {
        return new HotWater(
            new Temperature(),
            new WaterCirculation(
                new Temperature(),
                Pump.builder().name(PumpType.CIRCULATION_PUMP).build()
            )
        );
    }

    @Bean
    Furnace furnace() {
        return new Furnace();
    }

    @Bean
    Pump heatingPump() {
        return Pump.builder().name(PumpType.HEATING_PUMP).build();
    }

    @Bean
    Pump hotWaterPump() {
        return Pump.builder().name(PumpType.HOT_WATER_PUMP).build();
    }

    @Bean
    Pump fireplacePump() {
        return Pump.builder().name(PumpType.FIREPLACE_PUMP).build();
    }

    @Bean
    Pump floorPump() {
        return Pump.builder().name(PumpType.FLOOR_PUMP).build();
    }

    @Bean
    List<Pump> pumps(
        final Pump heatingPump,
        final Pump hotWaterPump,
        final Pump fireplacePump,
        final Pump floorPump
    ) {
        return List.of(heatingPump, hotWaterPump, fireplacePump, floorPump);
    }
}
