package app.api.diagnosticruntime.annotation.ai.model;

import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "pending_ai_annotations")
public class PendingAIAnnotation {
    @Id
    private String id;

    private String slideId;

    private String slideImagePath;

    private CaseType caseType;

    private TaskStatus status;

    private LocalDateTime createdAt;
}
