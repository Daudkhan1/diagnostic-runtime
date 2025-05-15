package app.api.diagnosticruntime.organ.repository;

import app.api.diagnosticruntime.organ.model.Organ;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OrganRepository extends MongoRepository<Organ, String> {
    Optional<Organ> findByName(String name);
} 