package cloud.cholewa.heating.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HeatingStatusReply(
    boolean isHeatingEnabled,
    LocalDateTime updatedAt
) {
}
