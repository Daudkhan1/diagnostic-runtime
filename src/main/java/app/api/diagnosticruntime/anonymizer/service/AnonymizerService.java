package app.api.diagnosticruntime.anonymizer.service;

import app.api.diagnosticruntime.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnonymizerService {

    @Value("${anonymizer.service.url}")
    private String anonymizerServiceUrl;

    private final AwsProperties awsProperties;
    private final RestTemplate restTemplate;

    public String anonymizePathologyFile(String newFileName) {
        return anonymize(newFileName, "/process-image/");
    }

    public String anonymizeRadiologyFile(String newFileName) {
        return anonymize(newFileName, "/process-dicom/");
    }

    public String anonymize(String newFileName, String apiPoint) {

        log.info("Anonymizing file : {}", newFileName);
        log.info("AnonymizerServiceUrl is : {}", anonymizerServiceUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        // Create request body with s3_uri
        String s3URI = "s3://" + awsProperties.getBucketName() + "/" + newFileName;
        log.info("S3 URI is: {}", s3URI);
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("s3_uri", s3URI);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            anonymizerServiceUrl + apiPoint,
            HttpMethod.POST,
            entity,
            String.class
        );
        log.info("The response is : {}", response);
        log.info("The response body is : {}", response.getBody());
        return response.getBody();
    }
} 