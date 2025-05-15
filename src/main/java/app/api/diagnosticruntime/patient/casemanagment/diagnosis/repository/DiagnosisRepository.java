package app.api.diagnosticruntime.patient.casemanagment.diagnosis.repository;


import app.api.diagnosticruntime.patient.casemanagment.diagnosis.model.Diagnosis;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface DiagnosisRepository extends MongoRepository<Diagnosis, String> {
    List<Diagnosis> findByIsDeleted(boolean deleted);

    List<Diagnosis> findByUserIdAndCaseIdAndIsDeleted(String userId, String caseId, boolean deleted);

    List<Diagnosis> findByCaseIdAndIsDeleted(String caseId, boolean deleted);
}