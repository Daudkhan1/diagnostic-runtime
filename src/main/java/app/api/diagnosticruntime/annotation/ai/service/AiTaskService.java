package app.api.diagnosticruntime.annotation.ai.service;

import app.api.diagnosticruntime.annotation.dto.AIAnnotatonCreationDTO;
import app.api.diagnosticruntime.annotation.service.AnnotationService;
import app.api.diagnosticruntime.config.Ec2Properties;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.patient.model.PatientSlide;
import app.api.diagnosticruntime.patient.service.PatientSlideService;
import app.api.diagnosticruntime.slides.service.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class AiTaskService {
    private final S3Service s3Service;
    private final AnnotationService annotationService;
    private final Ec2Properties ec2Properties;

    private final RestTemplate restTemplate;

    private final PatientSlideService patientSlideService;

    protected AiTaskService(S3Service s3Service, AnnotationService annotationService,
                            Ec2Properties ec2Properties, RestTemplate restTemplate,
                            PatientSlideService patientSlideService) {
        this.s3Service = s3Service;
        this.annotationService = annotationService;
        this.ec2Properties = ec2Properties;
        this.restTemplate = restTemplate;
        this.patientSlideService = patientSlideService;
    }

    abstract void  enqueueTask(String slideId, String slideImagePath, CaseType caseType);

    public void createAIAnnotations(String slideId, String slideImagePath, CaseType caseType) throws Exception {

        String filenameWithoutExtension = slideImagePath.substring(0, slideImagePath.lastIndexOf('.'));

        String fileLocation = "processed/" + filenameWithoutExtension + "/processed." + "tiff";

        URL url = s3Service.generatePresignedUrl(fileLocation, 300);
        // Prepare the request payload
        PatientSlide patientSlide = patientSlideService.getPatientSlideById(slideId);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("image_path", url.toString());
        requestBody.put("patient_slide_id", slideId);
        requestBody.put("modality", caseType);

        String predictEndpoint = ec2Properties.getEndpoint();

        log.info("Sending request to AI model with PatientSlide{} and imagePath:{}", slideId, slideImagePath);
        AIAnnotatonCreationDTO response = restTemplate.postForObject(predictEndpoint, requestBody, AIAnnotatonCreationDTO.class);
        if (response != null) {
            annotationService.createAIAnnotations(response, patientSlide, caseType.equals(CaseType.RADIOLOGY));
        }

    }
}
