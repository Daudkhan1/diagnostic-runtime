package app.api.diagnosticruntime.annotation.ai.service;

import app.api.diagnosticruntime.annotation.service.AnnotationService;
import app.api.diagnosticruntime.config.Ec2Properties;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.patient.service.PatientSlideService;
import app.api.diagnosticruntime.slides.service.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@Profile("without-ai-annotation")
public class NonPersistentAiTaskService extends AiTaskService {


    protected NonPersistentAiTaskService(
            S3Service s3Service,
            AnnotationService annotationService,
            Ec2Properties ec2Properties,
            RestTemplate restTemplate,
            PatientSlideService patientSlideService
    ) {
        super(s3Service, annotationService, ec2Properties, restTemplate, patientSlideService);
    }

    @Override
    public void enqueueTask(String slideId, String slideImagePath, CaseType caseType) {
        log.info("Task not enqueued for slideId: {} and case Type: {}",slideId, caseType);
    }
}
