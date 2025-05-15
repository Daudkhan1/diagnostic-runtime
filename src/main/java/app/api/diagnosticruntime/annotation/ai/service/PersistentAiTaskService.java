package app.api.diagnosticruntime.annotation.ai.service;

import app.api.diagnosticruntime.annotation.ai.model.PendingAIAnnotation;
import app.api.diagnosticruntime.annotation.ai.model.TaskStatus;
import app.api.diagnosticruntime.annotation.ai.repository.PendingAIAnnotationRepository;
import app.api.diagnosticruntime.annotation.service.AnnotationService;
import app.api.diagnosticruntime.config.Ec2Properties;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.patient.service.PatientSlideService;
import app.api.diagnosticruntime.slides.service.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Slf4j
@Service
@Profile("with-ai-annotation")
public class PersistentAiTaskService extends AiTaskService {

    private final PendingAIAnnotationRepository pendingTaskRepository;
    protected PersistentAiTaskService(
            S3Service s3Service,
            AnnotationService annotationService,
            Ec2Properties ec2Properties,
            PendingAIAnnotationRepository pendingTaskRepository,
            RestTemplate restTemplate,
            PatientSlideService patientSlideService
    ) {
        super(s3Service, annotationService, ec2Properties, restTemplate, patientSlideService);
        this.pendingTaskRepository = pendingTaskRepository;
    }

    @Override
    public void enqueueTask(String slideId, String slideImagePath, CaseType caseType) {
        log.info("Task enqueued for slideId: " + slideId);
        PendingAIAnnotation task = new PendingAIAnnotation();
        task.setStatus(TaskStatus.PENDING);
        task.setCreatedAt(LocalDateTime.now());
        task.setSlideId(slideId);
        task.setSlideImagePath(slideImagePath);
        task.setCaseType(caseType);
        pendingTaskRepository.save(task);
    }
}

