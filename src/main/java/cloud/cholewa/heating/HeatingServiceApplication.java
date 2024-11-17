package cloud.cholewa.heating;

import cloud.cholewa.heating.config.job.PumpsPoolingJobConfig;
import cloud.cholewa.heating.water.client.HotWaterSensorConfig;
import cloud.cholewa.heating.config.job.HotWaterSensorPoolingJobConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(
    {
        HotWaterSensorPoolingJobConfig.class,
        HotWaterSensorConfig.class,
        PumpsPoolingJobConfig.class
    }
)
@EnableScheduling
public class HeatingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeatingServiceApplication.class, args);
    }

}
