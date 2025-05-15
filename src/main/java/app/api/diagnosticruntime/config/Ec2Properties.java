package app.api.diagnosticruntime.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "ec2")
public class Ec2Properties {
    private String endpoint;
    private String region;
    private String diagnosticAiInstance;
    private String accessKeyId;
    private String secretKey;
}
