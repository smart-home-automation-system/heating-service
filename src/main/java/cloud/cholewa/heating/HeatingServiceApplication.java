package cloud.cholewa.heating;

import cloud.cholewa.heating.config.RelayConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({RelayConfig.class})
@EnableScheduling
public class HeatingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeatingServiceApplication.class, args);
    }

}
