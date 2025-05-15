package app.api.diagnosticruntime.annotation.ai.service;

import app.api.diagnosticruntime.annotation.ai.model.PendingAIAnnotation;
import app.api.diagnosticruntime.annotation.ai.model.TaskStatus;
import app.api.diagnosticruntime.annotation.ai.repository.PendingAIAnnotationRepository;
import app.api.diagnosticruntime.config.Ec2Properties;
import app.api.diagnosticruntime.config.repository.ConfigRepository;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.slides.service.EC2Service;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class TaskProcessorService {

    private final PendingAIAnnotationRepository pendingTaskRepository;
    private final EC2Service ec2Service;
    private final Ec2Properties ec2Properties;

    private final ConfigRepository configRepository;

    private final AiTaskService aiTaskService;

    private boolean ec2Active = false;
    private long lastTaskCompletionTime = System.currentTimeMillis();

    public TaskProcessorService(PendingAIAnnotationRepository pendingTaskRepository, EC2Service ec2Service,
                                Ec2Properties ec2Properties,
                                ConfigRepository configRepository, AiTaskService aiTaskService
    ) {
        this.pendingTaskRepository = pendingTaskRepository;
        this.ec2Service = ec2Service;
        this.ec2Properties = ec2Properties;
        this.configRepository = configRepository;
        this.aiTaskService = aiTaskService;
    }

    public void enqueueTask(String slideId, String slideImagePath, CaseType caseType) {
        aiTaskService.enqueueTask(slideId, slideImagePath, caseType);
    }

    public void startTaskProcessor() {
            try {
                boolean ec2TaskEnabled = getEc2TaskEnabled();
                if (!ec2TaskEnabled) {
                    log.info("EC2 task processing is disabled.");
//                        Thread.sleep(60000); // Wait before rechecking
//                        checkAndHibernateEC2();
                } else {
                    log.info("EC2 task processing is enabled.");
                    PendingAIAnnotation task = pendingTaskRepository.findFirstByStatusOrderByCreatedAt(TaskStatus.PENDING);
                    if (task != null) {
                        processTask(task);
                        lastTaskCompletionTime = System.currentTimeMillis();
                    }
//                        else {
//                        Thread.sleep(60000); // Wait before checking for new tasks
//                        checkAndHibernateEC2();
//                        }
                }

            } catch (Exception e) {
                log.error("Exception in the startTaskProcessor :{}",e.getMessage());
//                    checkAndHibernateEC2();
                e.printStackTrace();
            }

    }

    public void processTask(PendingAIAnnotation task) {
        try {
            task.setStatus(TaskStatus.PROCESSING);
            pendingTaskRepository.save(task);

            ensureEC2Running();

//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                throw new RuntimeException("Waiting for EC2 Dockers to start");
//            }

            // Perform heavy operation (not transactional)
            aiTaskService.createAIAnnotations(task.getSlideId(), task.getSlideImagePath(), task.getCaseType());

            // Update the task status in a separate transaction
            markTaskAsCompleted(task);
        } catch (Exception e) {
            log.error("Task with image: {}, casetype: {} and slide Id:{} failed to complete", task.getSlideImagePath(), task.getCaseType(), task.getSlideId());
            markTaskAsFailed(task);
            e.printStackTrace();
        }
    }

    public void markTaskAsCompleted(PendingAIAnnotation task) {
        log.info("Task of patient slide:{} with image: {} and type :{} COMPLETED", task.getSlideId(), task.getSlideImagePath(), task.getCaseType());
        task.setStatus(TaskStatus.COMPLETED);
        pendingTaskRepository.save(task);
    }

    public void markTaskAsFailed(PendingAIAnnotation task) {
        log.info("Task of patient slide:{} with image: {} and type :{} FAILED Retrying again", task.getSlideId(), task.getSlideImagePath(), task.getCaseType());
        task.setStatus(TaskStatus.PENDING);
        pendingTaskRepository.save(task);
    }

    private void ensureEC2Running() {
        if (!ec2Service.isInstanceRunning(ec2Properties.getDiagnosticAiInstance())) {
            ec2Service.startInstanceIfStopped(ec2Properties.getDiagnosticAiInstance());
            ec2Active = true;
        }
    }

    public void checkAndHibernateEC2() {
        log.info("Hibernating");
        long idleTime = System.currentTimeMillis() - lastTaskCompletionTime;
        long idleTimeoutMillis = 5 * 60 * 1000; // 5 minutes
        log.info("idleTime : {}", idleTime);
        log.info("idleTimeoutMillis : {}", idleTimeoutMillis);
        log.info("ec2Active : {}", ec2Active);
        log.info("idleTime > idleTimeoutMillis : {}", idleTime > idleTimeoutMillis);

        if (idleTime > idleTimeoutMillis) {
            boolean isActuallyRunning = ec2Service.isInstanceRunning(ec2Properties.getDiagnosticAiInstance());
            log.info("idleTime > idleTimeoutMillis and is Instance running : {}", isActuallyRunning);
            if(isActuallyRunning) {
                ec2Service.hibernateInstance(ec2Properties.getDiagnosticAiInstance());
                ec2Active = false;
            }
        }
    }

    private boolean getEc2TaskEnabled() {
        return configRepository.findByKey("ec2TaskEnabled")
                .map(config -> Boolean.parseBoolean(config.getValue()))
                .orElse(false); // Default to false if not present
    }
}

