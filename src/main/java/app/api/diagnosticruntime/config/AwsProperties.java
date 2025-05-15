package app.api.diagnosticruntime.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "aws")
public class AwsProperties {
    private String accessKeyId;
    private String bucketName;
    private String region;
    private String secretKey;

    public AwsBasicCredentials getCredentials() {
        return AwsBasicCredentials.create(accessKeyId, secretKey);
    }
}

