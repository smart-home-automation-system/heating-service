package cloud.cholewa.heating.config.job;

import jakarta.validation.constraints.NotNull;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("jobs.hot-water")
@Value
public class HotWaterSensorPoolingJobConfig {
    @NotNull
    Duration poolingInterval;
}
