package cloud.cholewa.heating.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Humidity {
    private double value;
    private LocalDateTime updatedAt;

    //a value and its time are one reading (HAS-360: the humidity of a Shelly thermometer), written and read as one
    public synchronized void update(final double newValue, final LocalDateTime newUpdatedAt) {
        this.value = newValue;
        this.updatedAt = newUpdatedAt;
    }

    //the reading only when it is newer than the one held: a BLU H&T is read again from its gateway every five minutes
    public synchronized boolean updateIfNewer(final double newValue, final LocalDateTime newUpdatedAt) {
        if (updatedAt != null && !newUpdatedAt.isAfter(updatedAt)) {
            return false;
        }
        update(newValue, newUpdatedAt);
        return true;
    }

    public synchronized Humidity snapshot() {
        final Humidity copy = new Humidity();
        copy.value = value;
        copy.updatedAt = updatedAt;
        return copy;
    }
}
