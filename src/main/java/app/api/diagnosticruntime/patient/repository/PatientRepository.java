package app.api.diagnosticruntime.patient.repository;

import app.api.diagnosticruntime.patient.model.Patient;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PatientRepository extends MongoRepository<Patient, String> {

    Optional<Patient> findByMrn(String s);
    Optional<Patient> findByPraidId(Long praidId);
    Optional<Patient> findTopByOrderByPraidIdDesc();
}
