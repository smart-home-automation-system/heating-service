package cloud.cholewa.heating.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FloorPump {
    boolean isWorking;
    LocalDateTime updatedAt;
}
