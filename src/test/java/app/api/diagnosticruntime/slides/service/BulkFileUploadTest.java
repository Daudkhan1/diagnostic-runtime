package app.api.diagnosticruntime.slides.service;

import app.api.diagnosticruntime.anonymizer.service.AnonymizerService;
import app.api.diagnosticruntime.config.AwsProperties;
import app.api.diagnosticruntime.config.MongoDBTestContainer;
import app.api.diagnosticruntime.config.TestAwsConfig;
import app.api.diagnosticruntime.util.FileUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestAwsConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = {
    "spring.test.database.replace=none",
//    "aws.accessKeyId=${AWS_ACCESS_KEY_ID}",
//    "aws.secretKey=${AWS_SECRET_ACCESS_KEY}",
//    "aws.region=${AWS_REGION}",
//    "aws.bucketName=${AWS_BUCKET_NAME}"
})
public class BulkFileUploadTest {

    @Autowired
    private S3Service s3Service;

    @Autowired
    private AnonymizerService anonymizerService;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private AwsProperties awsProperties;

    @BeforeAll
    static void startContainer() {
        MongoDBTestContainer.start();
    }

    @AfterAll
    static void stopContainer() {
        MongoDBTestContainer.stop();
    }

    private Set<String> getExistingProcessedFolders() {
        Set<String> existingFolders = new HashSet<>();
        String prefix = "processed/";
        
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(awsProperties.getBucketName())
                .prefix(prefix)
                .delimiter("/")
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(request);
        
        // Get all common prefixes (folders)
        if (response.commonPrefixes() != null) {
            response.commonPrefixes().forEach(prefixObj -> {
                String folderPath = prefixObj.prefix();
                // Remove "processed/" prefix and trailing slash
                String folderName = folderPath.substring(prefix.length(), folderPath.length() - 1);
                existingFolders.add(folderName);
            });
        }

        return existingFolders;
    }

    @Test
    @Order(1)
    public void testBulkFileUploadAndAnonymization() throws Exception {
        // Configuration
        String sourceDirectory = "C:/Users/Computer/Desktop/files"; // Replace with your source directory
        int maxConcurrentUploads = 5; // Adjust based on your system's capabilities
        List<String> uploadedFiles = new ArrayList<>();

        // Get existing processed folders from S3
        Set<String> existingFolders = getExistingProcessedFolders();
        System.out.println("Found " + existingFolders.size() + " existing processed folders in S3");

        // Get all files from the directory
        List<File> files = Files.walk(Paths.get(sourceDirectory))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .collect(Collectors.toList());

        System.out.println("Found " + files.size() + " files to process");

        // Filter out files that already have processed folders
        List<File> filesToProcess = files.stream()
                .filter(file -> {
                    String fileNameWithoutExtension = file.getName().substring(0, file.getName().lastIndexOf('.'));
                    boolean shouldProcess = !existingFolders.contains(fileNameWithoutExtension);
                    if (!shouldProcess) {
                        System.out.println("Skipping " + file.getName() + " - already processed");
                    }
                    return shouldProcess;
                })
                .collect(Collectors.toList());

        System.out.println("After filtering, " + filesToProcess.size() + " files need to be processed");

        // Create thread pool for concurrent uploads
        ExecutorService executorService = Executors.newFixedThreadPool(maxConcurrentUploads);
        List<CompletableFuture<String>> uploadFutures = new ArrayList<>();

        // Upload files concurrently
        for (File file : filesToProcess) {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    String fileName = file.getName();
                    System.out.println("Uploading file: " + fileName);

                    // Create MultipartFile from File
                    FileInputStream input = new FileInputStream(file);
                    MultipartFile multipartFile = new MockMultipartFile(
                            fileName,
                            fileName,
                            Files.probeContentType(file.toPath()),
                            input
                    );

                    // Upload file
                    String result = s3Service.uploadLargeFile(multipartFile);
                    System.out.println("Successfully uploaded: " + fileName);
                    uploadedFiles.add(fileName);
                    return result;
                } catch (Exception e) {
                    System.err.println("Error uploading file " + file.getName() + ": " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }, executorService);

            uploadFutures.add(future);
        }

        // Wait for all uploads to complete
        CompletableFuture.allOf(uploadFutures.toArray(new CompletableFuture[0])).join();
        executorService.shutdown();

        System.out.println("All files uploaded successfully. Starting anonymization...");

        // Anonymize files sequentially to avoid overwhelming the anonymization service
        for (String fileName : uploadedFiles) {
            try {
                System.out.println("Anonymizing file: " + fileName);
                String result = anonymizerService.anonymizePathologyFile(fileName);
                System.out.println("Successfully anonymized: " + fileName);
                System.out.println("Anonymization result: " + result);
            } catch (Exception e) {
                System.err.println("Error anonymizing file " + fileName + ": " + e.getMessage());
            }
        }

        System.out.println("Bulk upload and anonymization completed!");
    }
} 