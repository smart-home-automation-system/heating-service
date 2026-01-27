package cloud.cholewa.heating.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HomeStatus {
    boolean isHomeHeatingEnabled;
    LocalDateTime updatedAt;
}
