package cloud.cholewa.heating;

import cloud.cholewa.heating.config.NotificationConnectionProperties;
import cloud.cholewa.heating.config.RelayConfig;
import cloud.cholewa.heating.config.SensorMonitorProperties;
import cloud.cholewa.heating.config.ShellyTimeoutProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
    RelayConfig.class,
    SensorMonitorProperties.class,
    NotificationConnectionProperties.class,
    ShellyTimeoutProperties.class
})
public class HeatingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeatingServiceApplication.class, args);
    }

}
