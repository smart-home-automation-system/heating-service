package cloud.cholewa.heating.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FloorPump {
    volatile boolean isWorking;
    volatile LocalDateTime updatedAt;
}
