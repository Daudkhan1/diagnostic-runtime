package app.api.diagnosticruntime.slides.service;

import app.api.diagnosticruntime.config.AwsProperties;
import app.api.diagnosticruntime.slides.dto.AmazonFilePart;
import app.api.diagnosticruntime.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Transactional
@Service
public class S3Service {

    private final AwsProperties awsProperties;

    private final S3Client s3Client;

    private final S3Presigner s3Presigner;

    private final FileUtils fileUtils;


    public S3Service(AwsProperties awsProperties, FileUtils fileUtils) {
        this.awsProperties = awsProperties;
        this.fileUtils = fileUtils;
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(awsProperties.getAccessKeyId(), awsProperties.getSecretKey());
        this.s3Client = S3Client.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();

        this.s3Presigner = S3Presigner.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(awsProperties.getAccessKeyId(), awsProperties.getSecretKey())))
                .build();
    }

    public URL generatePresignedUrl(String filePath, int expirationMinutes) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(awsProperties.getBucketName())
                .key(filePath)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url();
    }

    public String initiateMultipartUpload(String fileName) {
        CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(awsProperties.getBucketName())
                .key(fileName)
                .build();
        log.info("Getting multipart upload Response...");
        CreateMultipartUploadResponse createMultipartUploadResponse = s3Client.createMultipartUpload(createMultipartUploadRequest);
        String uploadId = createMultipartUploadResponse.uploadId();
        return uploadId;
    }

    public AmazonFilePart uploadPart(String fileName, String uploadId, int partNumber, MultipartFile file) throws IOException {

        UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                .bucket(awsProperties.getBucketName())
                .key(fileName)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .contentLength(file.getSize())
                .build();

        UploadPartResponse uploadPartResponse = s3Client.uploadPart(uploadPartRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return new AmazonFilePart(uploadPartResponse.eTag(), partNumber);
    }

    public void abortMultipartUpload(String fileName, String uploadId) {
        AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                .bucket(awsProperties.getBucketName())
                .uploadId(uploadId)
                .key(fileName)
                .build();
        s3Client.abortMultipartUpload(abortRequest);
    }


    public String uploadLargeFile(MultipartFile file) throws Exception {

        log.info("Creating multipart upload request...");
        String keyName = file.getOriginalFilename();
        CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(awsProperties.getBucketName())
                .key(keyName)
                .build();
        log.info("Getting multipart upload Response...");
        CreateMultipartUploadResponse createMultipartUploadResponse = s3Client.createMultipartUpload(createMultipartUploadRequest);
        String uploadId = createMultipartUploadResponse.uploadId();
        log.info("Multipart upload Id..." + uploadId);

        List<CompletableFuture<CompletedPart>> uploadFutures = new ArrayList<>();
        int partSize = 10 * 1024 * 1024; // 5MB per part
        int partNumber = 1;

        log.info("Initializing Executor");
        ExecutorService executorService = Executors.newFixedThreadPool(10); // Adjust the thread pool size as needed
        log.info("Uploading...");
        try (InputStream inputStream = file.getInputStream()) {
            int bytesRead;
            byte[] buffer = new byte[partSize];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] partData = Arrays.copyOfRange(buffer, 0, bytesRead);
                int currentPartNumber = partNumber;
                long contentLengthForLambda = bytesRead;

                CompletableFuture<CompletedPart> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                                .bucket(awsProperties.getBucketName())
                                .key(keyName)
                                .uploadId(uploadId)
                                .partNumber(currentPartNumber)
                                .contentLength(contentLengthForLambda)
                                .build();

                        UploadPartResponse uploadPartResponse = s3Client.uploadPart(uploadPartRequest, RequestBody.fromBytes(partData));
//                        log.info("Uploaded part number: " + currentPartNumber + ", ETag: " + uploadPartResponse.eTag());

                        return CompletedPart.builder()
                                .partNumber(currentPartNumber)
                                .eTag(uploadPartResponse.eTag())
                                .build();
                    } catch (Exception e) {
                        log.error("Error uploading part number: " + currentPartNumber, e);
                        throw new RuntimeException(e);
                    }
                }, executorService);

                uploadFutures.add(future);
                partNumber++;
            }

            // Wait for all uploads to complete and collect the parts
            List<CompletedPart> completedParts = uploadFutures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            // Step 3: Complete the multipart upload
            CompleteMultipartUploadRequest completeMultipartUploadRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(awsProperties.getBucketName())
                    .key(keyName)
                    .uploadId(uploadId)
                    .multipartUpload(multipartUpload -> multipartUpload.parts(completedParts))
                    .build();
            log.info("Completing multipart upload...");
            s3Client.completeMultipartUpload(completeMultipartUploadRequest);

            return "File uploaded successfully: " + keyName;
        } catch (Exception e) {
            log.error("Error during file upload: ", e);
            // Abort the multipart upload in case of failure
            s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(awsProperties.getBucketName())
                    .key(keyName)
                    .uploadId(uploadId)
                    .build());
            throw e;
        } finally {
            executorService.shutdown();
        }
    }

    public Map<String, Object> generatePresignedUrls(String fileName, long fileSize, Integer chunkPartition) {
        // Step 1: Initiate a multipart upload and get the upload ID
        CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(awsProperties.getBucketName())
                .key(fileName)
                .build();
        CreateMultipartUploadResponse createMultipartUploadResponse = s3Client.createMultipartUpload(createMultipartUploadRequest);
        String uploadId = createMultipartUploadResponse.uploadId();

        // Step 2: Generate presigned URLs for each part
        List<String> presignedUrls = new ArrayList<>();
        long chunkSize = chunkPartition * 1024 * 1024;
        int totalChunks = (int) Math.ceil((double) fileSize / chunkSize);

        for (int i = 1; i <= totalChunks; i++) {
            UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                    .bucket(awsProperties.getBucketName())
                    .key(fileName)
                    .uploadId(uploadId)
                    .partNumber(i)
                    .build();

            PresignedUploadPartRequest presignedRequest = s3Presigner.presignUploadPart(builder -> builder
                    .uploadPartRequest(uploadPartRequest)
                    .signatureDuration(Duration.ofHours(1))
            );

            URL url = presignedRequest.url();
            presignedUrls.add(url.toString());
        }

        // Return both the uploadId and the list of presigned URLs
        Map<String, Object> response = new HashMap<>();
        response.put("uploadId", uploadId);
        response.put("presignedUrls", presignedUrls);

        return response;
    }

    public void completeMultiPartUploadWithAnonymization(String fileName, String newFileName, String uploadId, List<AmazonFilePart> completedParts) {

        // 1. Validate and possibly convert filename
        String tiffCompliantFileName = FileUtils.toTiffCompliantFileName(newFileName);

        List<CompletedPart> completedFileParts = completedParts.stream()
                .map(completedPart -> CompletedPart.builder()
                        .eTag(completedPart.getEtag())
                        .partNumber(completedPart.getPartNumber())
                        .build())
                .sorted(Comparator.comparing(CompletedPart::partNumber))
                .collect(Collectors.toList());


        CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                .bucket(awsProperties.getBucketName())
                .key(fileName)
                .uploadId(uploadId)
                .multipartUpload(m -> m.parts(completedFileParts))
                .build();
        s3Client.completeMultipartUpload(completeRequest);

        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(awsProperties.getBucketName())
                .sourceKey(fileName)
                .destinationBucket(awsProperties.getBucketName())
                .destinationKey(tiffCompliantFileName)
                .build();
        s3Client.copyObject(copyRequest);

        // Step 2: Delete the original file
        deleteFile(fileName);

    }

    public void completeMultiPartUploadWithoutAnonymization(String fileName, String newFileName, String uploadId, List<AmazonFilePart> completedParts) {
        // 1. Validate and possibly convert filename
        String tiffCompliantFileName = FileUtils.toTiffCompliantFileName(newFileName);
        
        // 2. Extract filename without extension and create new path
        String filenameWithoutExtension = tiffCompliantFileName.substring(0, tiffCompliantFileName.lastIndexOf('.'));
        String processedFilePath = "processed/" + filenameWithoutExtension + "/processed.tiff";

        List<CompletedPart> completedFileParts = completedParts.stream()
                .map(completedPart -> CompletedPart.builder()
                        .eTag(completedPart.getEtag())
                        .partNumber(completedPart.getPartNumber())
                        .build())
                .sorted(Comparator.comparing(CompletedPart::partNumber))
                .collect(Collectors.toList());

        CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                .bucket(awsProperties.getBucketName())
                .key(fileName)
                .uploadId(uploadId)
                .multipartUpload(m -> m.parts(completedFileParts))
                .build();
        s3Client.completeMultipartUpload(completeRequest);

        // Copy to the new processed path structure
        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(awsProperties.getBucketName())
                .sourceKey(fileName)
                .destinationBucket(awsProperties.getBucketName())
                .destinationKey(processedFilePath)
                .build();
        s3Client.copyObject(copyRequest);

        // Delete the original file
        deleteFile(fileName);
    }

    public void deleteFile(String fileName) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(awsProperties.getBucketName())
                .key(fileName)
                .build();
        s3Client.deleteObject(deleteRequest);
    }

    public String getS3UrlFromFile(ByteArrayOutputStream byteArrayOutputStream) {
        String fileName = "PRAID_REPORT_"+ UUID.randomUUID()+".pdf";

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(awsProperties.getBucketName())
                .key(fileName)
                .contentType("application/pdf")
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(byteArrayOutputStream.toByteArray()));
        URL url = generatePresignedUrl(fileName, 30);

        // Generate pre-signed URL
        return url.toString();
    }

    public String getBucketName() {
        return awsProperties.getBucketName();
    }
}
