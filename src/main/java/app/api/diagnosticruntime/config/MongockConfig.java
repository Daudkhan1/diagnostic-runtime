package app.api.diagnosticruntime.config;

import io.mongock.driver.api.driver.ConnectionDriver;
import io.mongock.runner.springboot.EnableMongock;
import io.mongock.runner.springboot.MongockSpringboot;
import io.mongock.runner.springboot.base.MongockInitializingBeanRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableMongock
public class MongockConfig {

    @Bean
    public MongockInitializingBeanRunner mongockRunner(ConnectionDriver driver, ApplicationContext applicationContext) {
        return MongockSpringboot.builder()
                .setDriver(driver)
                .setSpringContext(applicationContext)
                .addMigrationScanPackage("app.api.diagnosticruntime")
                .buildInitializingBeanRunner();
    }
}
