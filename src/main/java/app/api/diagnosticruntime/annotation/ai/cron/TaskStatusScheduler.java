package app.api.diagnosticruntime.annotation.ai.cron;

import app.api.diagnosticruntime.annotation.ai.model.PendingAIAnnotation;
import app.api.diagnosticruntime.annotation.ai.model.TaskStatus;
import app.api.diagnosticruntime.annotation.ai.repository.PendingAIAnnotationRepository;
import app.api.diagnosticruntime.annotation.ai.service.TaskProcessorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class TaskStatusScheduler {

    private final PendingAIAnnotationRepository pendingTaskRepository;

    private final TaskProcessorService taskProcessorService;

    public TaskStatusScheduler(PendingAIAnnotationRepository pendingTaskRepository,
                               TaskProcessorService taskProcessorService
    ) {
        this.pendingTaskRepository = pendingTaskRepository;
        this.taskProcessorService = taskProcessorService;
    }

    @Scheduled(cron = "0 0 * * * *") // Runs every 1 hour
    public void updateStaleProcessingTasks() {
        log.info("Running task to check for stale PROCESSING tasks...");

        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(60);

        // Find tasks with status PROCESSING and created more than 30 minutes ago
        List<PendingAIAnnotation> staleTasks = pendingTaskRepository.findByStatusAndCreatedAtBefore(TaskStatus.PROCESSING, cutoffTime);

        staleTasks.forEach(task -> {
            task.setStatus(TaskStatus.PENDING);
            pendingTaskRepository.save(task);
            log.info("Updated task with ID {} to PENDING.", task.getId());
        });

        log.info("Finished updating stale tasks. Total tasks updated: {}", staleTasks.size());
    }

    @Scheduled(fixedRate = 60000) // Runs every 1 hour
    public void processTasks() {
        log.info("Running tasks cron job");
        taskProcessorService.startTaskProcessor();
    }
}