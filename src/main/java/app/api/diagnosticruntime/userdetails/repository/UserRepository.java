package app.api.diagnosticruntime.userdetails.repository;

import app.api.diagnosticruntime.userdetails.model.User;
import app.api.diagnosticruntime.userdetails.model.UserRole;
import app.api.diagnosticruntime.userdetails.model.UserStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmailAndStatusIs(String email, UserStatus userStatus);

    Optional<User> findByIdAndStatusIs(String id, UserStatus userStatus);

    boolean existsByEmail(String s);

    Optional<User> findByEmail(String email);

    long countByStatus(UserStatus status);

    List<User> findAllByRoleAndStatus(UserRole role, UserStatus status);

    long countAllByRoleAndStatus(UserRole role, UserStatus status);
    long countAllByRole(UserRole role);

}
