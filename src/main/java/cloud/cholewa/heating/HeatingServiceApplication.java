package cloud.cholewa.heating;

import cloud.cholewa.heating.config.DeviceConnectionProperties;
import cloud.cholewa.heating.config.InternalServiceProperties;
import cloud.cholewa.heating.config.NotificationConnectionProperties;
import cloud.cholewa.heating.config.RelayConfig;
import cloud.cholewa.heating.config.SensorMonitorProperties;
import cloud.cholewa.heating.config.ShellyTimeoutProperties;
import cloud.cholewa.heating.config.WindowProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

//scheduling is switched on by SchedulingConfig, outside the test profile
@SpringBootApplication
@EnableConfigurationProperties({
    RelayConfig.class,
    SensorMonitorProperties.class,
    NotificationConnectionProperties.class,
    ShellyTimeoutProperties.class,
    DeviceConnectionProperties.class,
    InternalServiceProperties.class,
    WindowProperties.class
})
public class HeatingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeatingServiceApplication.class, args);
    }

}
