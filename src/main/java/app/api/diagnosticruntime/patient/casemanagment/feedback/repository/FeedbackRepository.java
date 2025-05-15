package app.api.diagnosticruntime.patient.casemanagment.feedback.repository;

import app.api.diagnosticruntime.patient.casemanagment.feedback.model.Feedback;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FeedbackRepository extends MongoRepository<Feedback, String> {
    List<Feedback> findByIsDeleted(boolean deleted);
    List<Feedback> findByUserIdAndCaseIdAndIsDeleted(String userId, String caseId, boolean deleted);
    List<Feedback> findByCaseIdAndIsDeleted(String caseId, boolean deleted);
}