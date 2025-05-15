package app.api.diagnosticruntime.patient.casemanagment.feedback.dto;

import lombok.Data;

@Data
public class FeedbackDTO {
    private String id;
    private String caseId;
    private String userId;
    private int difficultyLevel;
    private String feedback;
    private boolean isDeleted;
}
