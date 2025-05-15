package app.api.diagnosticruntime.userdetails.repository;

import app.api.diagnosticruntime.userdetails.model.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RoleRepository extends MongoRepository<Role, Long> {

    Optional<Role> findByRoleName(String roleName);

}
