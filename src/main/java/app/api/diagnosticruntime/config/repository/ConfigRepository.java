package app.api.diagnosticruntime.config.repository;

import app.api.diagnosticruntime.config.model.Config;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
public interface ConfigRepository extends MongoRepository<Config, String> {
    Optional<Config> findByKey(String key); // Fetch a Config document by the key field
}
