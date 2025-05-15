package app.api.diagnosticruntime.patient.repository;

import app.api.diagnosticruntime.patient.model.PatientSlide;
import app.api.diagnosticruntime.patient.model.PatientSlideStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PatientSlideRepository extends MongoRepository<PatientSlide, String> {

    List<PatientSlide> findAllByCaseIdAndIsDeleted(String caseId, boolean deleted);

    long countAllByCaseIdAndIsDeleted(String caseId, boolean deleted);
    long countAllByCaseIdAndIsDeletedAndStatus(String caseId, boolean deleted, PatientSlideStatus status);

    void deleteAllByCaseId(String caseId);

    Optional<PatientSlide> getPatientSlideByIdAndIsDeleted(String id, boolean deleted);
}
