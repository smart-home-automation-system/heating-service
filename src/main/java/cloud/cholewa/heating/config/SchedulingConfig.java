package cloud.cholewa.heating.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

//Off under the test profile: a @SpringBootTest context would otherwise run what the application
//schedules - the hourly check of the temperature sensors and, from the start, the read of the
//window sensors (HAS-232)
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@Profile("!test")
public class SchedulingConfig {
}
