package cloud.cholewa.heating.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Temperature {
    private volatile double value;
    private volatile LocalDateTime updatedAt;

    //A value and its time are one reading. The three methods below share the lock of this object,
    //so a reader never gets half of a reading and a restored value cannot land inside a live one;
    //the plain setters remain for building a state in tests

    public synchronized void update(final double value, final LocalDateTime updatedAt) {
        this.value = value;
        this.updatedAt = updatedAt;
    }

    /**
     * Sets the reading only when there is none yet; a reading that is already there is newer.
     */
    public synchronized boolean updateIfAbsent(final double value, final LocalDateTime updatedAt) {
        if (this.updatedAt != null) {
            return false;
        }
        update(value, updatedAt);
        return true;
    }

    public synchronized Temperature snapshot() {
        return new Temperature(value, updatedAt);
    }
}
