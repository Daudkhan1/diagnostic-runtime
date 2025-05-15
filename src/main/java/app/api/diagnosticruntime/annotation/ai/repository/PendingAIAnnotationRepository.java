package app.api.diagnosticruntime.annotation.ai.repository;

import app.api.diagnosticruntime.annotation.ai.model.PendingAIAnnotation;
import app.api.diagnosticruntime.annotation.ai.model.TaskStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PendingAIAnnotationRepository extends MongoRepository<PendingAIAnnotation, String> {
    PendingAIAnnotation findFirstByStatusOrderByCreatedAt(TaskStatus status);

    List<PendingAIAnnotation> findByStatusAndCreatedAtBefore(TaskStatus status, LocalDateTime createdAt);

}
