package cloud.cholewa.heating;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(
    {
//        HotWaterSensorPoolingJobConfig.class,
//        HotWaterSensorConfig.class,
//        PumpsPoolingJobConfig.class
    }
)
@EnableScheduling
public class HeatingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeatingServiceApplication.class, args);
    }

}
