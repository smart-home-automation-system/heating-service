package cloud.cholewa.heating.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * {@code working} - what the relay of the floor pump last reported.<br>
 * {@code updatedAt} - when it reported that.<br>
 * Both are missing until the relay has answered once since the service started.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FloorPumpReply(
    Boolean working,
    LocalDateTime updatedAt
) {
}
