package app.api.diagnosticruntime.report;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ReportRepository extends MongoRepository<Report, String> {
    Optional<Report> findByCaseId(String caseId);

    void deleteAllByCaseId(String caseId);
}
