package app.api.diagnosticruntime.patient.casemanagment.feedback.mapper;

import app.api.diagnosticruntime.patient.casemanagment.feedback.dto.FeedbackDTO;
import app.api.diagnosticruntime.patient.casemanagment.feedback.model.Feedback;

public class FeedbackMapper {

    public static FeedbackDTO toDTO(Feedback feedback) {
        FeedbackDTO dto = new FeedbackDTO();
        dto.setId(feedback.getId());
        dto.setCaseId(feedback.getCaseId());
        dto.setUserId(feedback.getUserId());
        dto.setDifficultyLevel(feedback.getDifficultyLevel());
        dto.setFeedback(feedback.getFeedback());
        dto.setDeleted(feedback.isDeleted());
        return dto;
    }

    public static Feedback toEntity(FeedbackDTO dto) {
        Feedback feedback = new Feedback();
        feedback.setId(dto.getId());
        feedback.setCaseId(dto.getCaseId());
        feedback.setUserId(dto.getUserId());
        feedback.setDifficultyLevel(dto.getDifficultyLevel());
        feedback.setFeedback(dto.getFeedback());
        feedback.setDeleted(dto.isDeleted());
        return feedback;
    }
}