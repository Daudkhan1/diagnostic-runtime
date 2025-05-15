package app.api.diagnosticruntime.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsConfig {

    @Bean
    public S3Client s3Client(AwsProperties awsProperties) {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
            awsProperties.getAccessKeyId(),
            awsProperties.getSecretKey()
        );

        return S3Client.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(AwsProperties awsProperties) {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
            awsProperties.getAccessKeyId(),
            awsProperties.getSecretKey()
        );

        return S3Presigner.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
    }
} 