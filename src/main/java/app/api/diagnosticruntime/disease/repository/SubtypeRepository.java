package app.api.diagnosticruntime.disease.repository;

import app.api.diagnosticruntime.disease.model.Subtype;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SubtypeRepository extends MongoRepository<Subtype, String> {
    List<Subtype> findByOrganId(String organId);
    boolean existsByNameAndOrganId(String name, String organId);
    Optional<Subtype> findByNameAndOrganId(String name, String organId);
}