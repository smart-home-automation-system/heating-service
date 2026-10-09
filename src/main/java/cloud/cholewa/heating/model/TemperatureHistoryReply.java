package cloud.cholewa.heating.model;

import cloud.cholewa.home.model.RoomName;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The temperature of a room over a range, averaged into buckets of one width.<br>
 * {@code from}, {@code to} - the range as it was asked for; it includes its start and not its end.<br>
 * {@code bucketSeconds} - the width of a bucket, chosen by the service from the length of the range.<br>
 * {@code points} - one per bucket that has a reading, oldest first. A bucket without a reading has
 * no point, so two points further apart than {@code bucketSeconds} are a gap in the readings.
 */
public record TemperatureHistoryReply(
    RoomName room,
    LocalDateTime from,
    LocalDateTime to,
    long bucketSeconds,
    List<Point> points
) {

    /**
     * {@code at} - the start of the bucket, aligned to the clock of the house.<br>
     * {@code value} - the average of the readings in the bucket, rounded to 2 decimals.
     */
    public record Point(LocalDateTime at, double value) {
    }
}
