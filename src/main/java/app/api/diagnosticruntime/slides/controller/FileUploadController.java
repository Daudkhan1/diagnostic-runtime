package app.api.diagnosticruntime.slides.controller;

import app.api.diagnosticruntime.anonymizer.service.AnonymizerService;
import app.api.diagnosticruntime.config.repository.ConfigRepository;
import app.api.diagnosticruntime.slides.dto.AmazonFilePart;
import app.api.diagnosticruntime.slides.service.S3Service;
import app.api.diagnosticruntime.util.FileUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private final S3Service s3Service;
    private final FileUtils fileUtils;

    private final ConfigRepository configRepository;

    private final AnonymizerService anonymizerService;
    private final S3Client s3Client;

    public FileUploadController(S3Service s3Service, FileUtils fileUtils,
                                ConfigRepository configRepository,
                                AnonymizerService anonymizerService,
                                S3Client s3Client) {
        this.s3Service = s3Service;
        this.fileUtils = fileUtils;
        this.configRepository = configRepository;
        this.anonymizerService = anonymizerService;
        this.s3Client = s3Client;
    }



    @PostMapping(path = "/image",consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @Operation(summary = "Upload a large image to S3", description = "Uploads a large image to the S3 bucket.")

    public ResponseEntity<String> uploadLargeImage(@Parameter(description = "File to upload", required = true, schema = @Schema(type = "string", format = "binary"))
                                                   @RequestPart MultipartFile file) {
        log.info("Upload Large Image");
        try {
            String response = s3Service.uploadLargeFile(file);
            anonymizerService.anonymizeRadiologyFile(file.getOriginalFilename());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to upload file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/generate-presigned-urls")
    public ResponseEntity<Map<String, Object>> getPresignedUrls(@RequestParam String fileName, @RequestParam long fileSize, @RequestParam Integer chunkSize) {
        try {
            Map<String, Object> response = s3Service.generatePresignedUrls(fileName, fileSize, chunkSize);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error generating presigned URLs: " + e.getMessage()));
        }
    }


    @PostMapping("/start")
    public ResponseEntity<String> startMultipartUpload(@RequestParam String fileName) {
        String uploadId = s3Service.initiateMultipartUpload(fileName);
        return ResponseEntity.ok(uploadId);
    }

    @PostMapping(path = {"/chunk"}, consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<AmazonFilePart> uploadChunk(
            @RequestParam String uploadId,
            @RequestParam int partNumber,
            @RequestParam("file") MultipartFile file,
            @RequestParam String fileName) throws IOException {

        AmazonFilePart completedPart = s3Service.uploadPart(fileName, uploadId, partNumber, file);
        return ResponseEntity.ok(completedPart);
    }

    @PostMapping("/abort")
    public ResponseEntity<Void> abortMultipartUpload(@RequestParam String uploadId, @RequestParam String fileName) {
        s3Service.abortMultipartUpload(fileName, uploadId);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/complete-multipart-upload")
    public ResponseEntity<String> completeMultipartUpload(
            @RequestParam String fileName,
            @RequestParam String newFileName,
            @RequestParam String uploadId,
            @RequestBody List<AmazonFilePart> completedParts) {
        try {
            log.info("Complete MultiPart Upload");
            String tiffCompliantFileName = FileUtils.toTiffCompliantFileName(newFileName);

            boolean anonymizerEnabled = getAnonymizerEnabled();
            if(anonymizerEnabled){
                s3Service.completeMultiPartUploadWithAnonymization(fileName, newFileName, uploadId, completedParts);
                log.info("Starting Anonymization");
                anonymizerService.anonymizePathologyFile(tiffCompliantFileName);
            } else {
                s3Service.completeMultiPartUploadWithoutAnonymization(fileName, newFileName, uploadId, completedParts);
            }


            log.info("Deleting original file");
            s3Service.deleteFile(tiffCompliantFileName);
            return ResponseEntity.ok("Multipart upload completed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error completing multipart upload: " + e.getMessage());
        }
    }

    private boolean getAnonymizerEnabled() {
        return configRepository.findByKey("anonymizer")
                .map(config -> Boolean.parseBoolean(config.getValue()))
                .orElse(false); // Default to false if not present
    }

    @PostMapping("/anonymize-file")
    public ResponseEntity<String> anonymizeFile(
            @RequestParam String fileName
           ) {
        try {
            log.info("Anonymize file");
            anonymizerService.anonymizePathologyFile(fileName);
            return ResponseEntity.ok("Anonymization completed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error completing anonymization : " + e.getMessage());
        }
    }

    @GetMapping("/check-file-exists")
    @Operation(summary = "Check if a file exists in S3", description = "Checks if a file exists in the processed folder of S3 bucket based on filename and size.")
    public ResponseEntity<Map<String, Object>> checkFileExists(
            @Parameter(description = "Name of the file to check", required = true)
            @RequestParam String fileName,
            @Parameter(description = "Size of the file in bytes", required = true)
            @RequestParam long fileSize) {
        try {
            log.info("Checking if file exists: {} with size: {}", fileName, fileSize);
            
            // Get filename without extension
            String fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));
            String processedFilePath = "processed/" + fileNameWithoutExtension + "/processed.tiff";
            
            try {
                HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                        .bucket(s3Service.getBucketName())
                        .key(processedFilePath)
                        .build();
                
                HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
                long existingFileSize = headObjectResponse.contentLength();
                
                Map<String, Object> response = new HashMap<>();
                response.put("exists", true);
                response.put("size", existingFileSize);
                response.put("sizeMatches", existingFileSize == fileSize);
                response.put("path", processedFilePath);
                
                return ResponseEntity.ok(response);
            } catch (NoSuchKeyException e) {
                Map<String, Object> response = new HashMap<>();
                response.put("exists", false);
                response.put("message", "File not found in processed folder");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Error checking file existence: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error checking file existence: " + e.getMessage()));
        }
    }

}
