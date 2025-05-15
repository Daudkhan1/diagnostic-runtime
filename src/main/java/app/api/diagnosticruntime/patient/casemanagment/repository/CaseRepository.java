package app.api.diagnosticruntime.patient.casemanagment.repository;

import app.api.diagnosticruntime.patient.casemanagment.model.Case;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CaseRepository extends MongoRepository<Case, String> {
    // Custom query methods can be defined here

    boolean existsByIdAndIsDeleted(String id, boolean deleted);
    Optional<Case> findByIdAndIsDeleted(String id, boolean deleted);
    boolean existsByNameAndIsDeleted(String name, boolean deleted);
    boolean existsByName(String s);

    long countByStatus(CaseStatus caseStatus);

    List<Case> findByDateBetween(LocalDate startDate, LocalDate endDate);

    long countByStatusAndCaseType(CaseStatus status, CaseType caseType);

    long countByCaseType(CaseType caseType);

    List<Case> findByCaseTypeAndIsDeleted(CaseType caseType, boolean deleted);
}
