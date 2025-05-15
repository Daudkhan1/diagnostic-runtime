package app.api.diagnosticruntime.patient.casemanagment.feedback.dto;

import lombok.Data;

@Data
public class FeedbackUpdateDTO {
    private String caseId;
    private int difficultyLevel;
    private String feedback;
}