package cloud.cholewa.heating.config;

import cloud.cholewa.heating.model.HomeStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HomeStatusConfig {
    
    @Bean
    HomeStatus homeStatus() {
        HomeStatus homeStatus = new HomeStatus();
        homeStatus.setHomeHeatingEnabled(false);
        return homeStatus;
    }
}
