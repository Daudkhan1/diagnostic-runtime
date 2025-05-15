package app.api.diagnosticruntime.annotation.ai.repository;

import app.api.diagnosticruntime.annotation.model.BiologicalType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BiologicalTypeRepository extends MongoRepository<BiologicalType, String> {
    List<BiologicalType> findByCategory(String category);

    Optional<BiologicalType> findByName(String name);

}
