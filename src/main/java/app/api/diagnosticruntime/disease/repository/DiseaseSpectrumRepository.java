package app.api.diagnosticruntime.disease.repository;

import app.api.diagnosticruntime.disease.model.DiseaseSpectrum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DiseaseSpectrumRepository extends MongoRepository<DiseaseSpectrum, String> {
    List<DiseaseSpectrum> findByOrganId(String organId);
    Optional<DiseaseSpectrum> findByOrganIdAndName(String organId, String name);

    boolean existsByNameAndOrganId(String name, String organId);
    Optional<DiseaseSpectrum> findByNameAndOrganId(String name, String id);
}