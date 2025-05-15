package app.api.diagnosticruntime.disease.repository;

import app.api.diagnosticruntime.disease.model.Grading;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GradingRepository extends MongoRepository<Grading, String> {
    List<Grading> findByDiseaseSpectrumId(String diseaseSpectrumId);
    List<Grading> findAllByDiseaseSpectrumIdIn(Collection<String> diseaseSpectrumIds);

    boolean existsByNameAndDiseaseSpectrumId(String name, String diseaseSpectrumId);
    Optional<Grading> findByNameAndDiseaseSpectrumId(String name, String diseaseSpectrumId);
}